package com.cloudempiere.ai.test.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.cloudempiere.ai.database.SecureDatabaseQueryExecutor;
import com.cloudempiere.ai.database.dto.ADColumnMeta;
import com.cloudempiere.ai.database.dto.ADTableMeta;
import com.cloudempiere.ai.provider.langchain4j.ERPTools;
import com.cloudempiere.ai.test.categories.UnitTest;

/**
 * When the database rejects the AI's SQL, the model must get the database's message and the table's real columns
 * back, so it can correct the query; only the stop after the last attempt may end the agent (CLD-2427). Before, a
 * "column pp.validfrom does not exist" became "Query execution failed" without the column, logged as SEVERE.
 */
@UnitTest
@DisplayName("SQL error feedback to the model")
class SqlErrorFeedbackTest {

    private static final String PG_MISSING_COLUMN = "ERROR: column pp.validfrom does not exist\n  Position: 8";

    @Nested
    @DisplayName("SecureDatabaseQueryExecutor.databaseRejection")
    class DatabaseRejection {

        @ParameterizedTest(name = "SQLState {0}")
        @ValueSource(strings = { "42703", "42P01", "42601", "42883", "22P02", "22012", "21000" })
        @DisplayName("returns the database message when the SQL itself is wrong")
        void rejectsModelErrors(String sqlState) {
            SQLException sql = new SQLException("ERROR: bad query\n  Position: 12", sqlState);

            assertThat(SecureDatabaseQueryExecutor.databaseRejection(sql)).isEqualTo("ERROR: bad query");
        }

        @Test
        @DisplayName("finds the SQLException in the cause chain and drops the Position line")
        void unwrapsCauseChain() {
            Exception wrapped = new RuntimeException("DB call failed",
                new IllegalStateException(new SQLException(PG_MISSING_COLUMN, "42703")));

            assertThat(SecureDatabaseQueryExecutor.databaseRejection(wrapped))
                .isEqualTo("ERROR: column pp.validfrom does not exist");
        }

        @ParameterizedTest(name = "SQLState {0}")
        @ValueSource(strings = { "08006", "57014", "42501", "25006", "XX000" })
        @DisplayName("keeps connection, timeout, privilege and internal errors as system errors")
        void keepsSystemErrors(String sqlState) {
            assertThat(SecureDatabaseQueryExecutor.databaseRejection(new SQLException("boom", sqlState))).isNull();
        }

        @Test
        @DisplayName("ignores errors without an SQLException")
        void ignoresNonSqlErrors() {
            assertThat(SecureDatabaseQueryExecutor.databaseRejection(new RuntimeException("boom"))).isNull();
            assertThat(SecureDatabaseQueryExecutor.databaseRejection(null)).isNull();
        }
    }

    @Nested
    @DisplayName("ERPTools.enrichDatabaseError")
    class EnrichDatabaseError {

        private final Map<String, ADTableMeta> tables = new HashMap<>();
        private final Function<String, ADTableMeta> lookup = name -> tables.get(name.toUpperCase());

        EnrichDatabaseError() {
            // M_ProductPrice as ADSchemaCache now loads it: physical columns only, no virtual ValidFrom
            tables.put("M_PRODUCTPRICE", table("M_ProductPrice",
                key("M_ProductPrice_ID"), fk("M_PriceList_Version_ID"), fk("M_Product_ID"),
                col("PriceList"), col("PriceStd"), col("PriceLimit")));
            tables.put("M_PRICELIST_VERSION", table("M_PriceList_Version",
                key("M_PriceList_Version_ID"), fk("M_PriceList_ID"), col("Name"), col("ValidFrom")));
        }

        @Test
        @DisplayName("adds the aliased table's columns to a missing-column error")
        void enrichesAliasedColumn() {
            String sql = "SELECT pp.PriceStd, pp.ValidFrom FROM M_ProductPrice pp "
                + "JOIN M_PriceList_Version plv ON (pp.M_PriceList_Version_ID = plv.M_PriceList_Version_ID)";

            String message = ERPTools.enrichDatabaseError("ERROR: column pp.validfrom does not exist", sql, lookup);

            assertThat(message)
                .startsWith("ERROR: column pp.validfrom does not exist")
                .contains("Column 'validfrom' does not exist in table 'M_ProductPrice'")
                .contains("Available columns in M_ProductPrice:")
                .contains("M_PriceList_Version_ID", "PriceStd")
                .doesNotContain("ValidFrom,", "ValidFrom]");
        }

        @Test
        @DisplayName("suggests close column names")
        void suggestsSimilarColumns() {
            String sql = "SELECT pp.PriceStdd FROM M_ProductPrice pp";

            String message = ERPTools.enrichDatabaseError("ERROR: column pp.pricestdd does not exist", sql, lookup);

            assertThat(message).contains("Did you mean:").contains("PriceStd");
        }

        @Test
        @DisplayName("uses the only table of the query for an unqualified column")
        void unqualifiedColumnSingleTable() {
            String sql = "SELECT ValidFrom FROM M_ProductPrice";

            String message = ERPTools.enrichDatabaseError("ERROR: column \"validfrom\" does not exist", sql, lookup);

            assertThat(message).contains("Available columns in M_ProductPrice:");
        }

        @Test
        @DisplayName("does not read a column ending in 'From' as the FROM clause")
        void columnEndingInFrom() {
            // before the word boundary, "ValidFrom FROM M_PriceList_Version plv" parsed as table FROM, alias M_PriceList_Version
            String sql = "SELECT plv.ValidFrom FROM M_PriceList_Version plv WHERE plv.IsActive = 'Y'";

            String message = ERPTools.enrichDatabaseError("ERROR: column plv.isactive does not exist", sql, lookup);

            assertThat(message).contains("Available columns in M_PriceList_Version:");
        }

        @Test
        @DisplayName("leaves an unqualified column alone when the query has several tables")
        void unqualifiedColumnSeveralTables() {
            String sql = "SELECT ValidFrom FROM M_ProductPrice pp "
                + "JOIN M_PriceList_Version plv ON (pp.M_PriceList_Version_ID = plv.M_PriceList_Version_ID)";
            String error = "ERROR: column \"validfrom\" does not exist";

            assertThat(ERPTools.enrichDatabaseError(error, sql, lookup)).isEqualTo(error);
        }

        @Test
        @DisplayName("returns other errors and unknown tables unchanged")
        void leavesOtherErrorsAlone() {
            String sql = "SELECT x.Foo FROM Unknown_Table x";

            assertThat(ERPTools.enrichDatabaseError("ERROR: syntax error at or near \"FROM\"", sql, lookup))
                .isEqualTo("ERROR: syntax error at or near \"FROM\"");
            assertThat(ERPTools.enrichDatabaseError("ERROR: column x.foo does not exist", sql, lookup))
                .isEqualTo("ERROR: column x.foo does not exist");
            assertThat(ERPTools.enrichDatabaseError(null, sql, lookup)).isNull();
        }
    }

    @Nested
    @DisplayName("ERPTools.TOOL_ERROR_HANDLER")
    class ToolErrorHandler {

        @Test
        @DisplayName("ends the agent run when the query attempts are used up")
        void stopsOnExhaustedAttempts() {
            RuntimeException wrapped = new RuntimeException(new ERPTools.QueryAttemptsExhaustedException("stop"));

            assertThatThrownBy(() -> ERPTools.TOOL_ERROR_HANDLER.handle(wrapped, null))
                .isInstanceOf(ERPTools.QueryAttemptsExhaustedException.class)
                .hasMessage("stop");
        }

        @Test
        @DisplayName("returns any other tool error to the model as text")
        void returnsOtherErrorsAsText() {
            assertThat(ERPTools.TOOL_ERROR_HANDLER.handle(new IllegalStateException("boom"), null).text())
                .isEqualTo("boom");
            assertThat(ERPTools.TOOL_ERROR_HANDLER.handle(new IllegalStateException(), null).text())
                .isEqualTo(IllegalStateException.class.getName());
        }
    }

    // ------------------------------------------------------------------------

    private static ADTableMeta table(String name, ADColumnMeta... columns) {
        return new ADTableMeta(0, name, name, null, "3", false, Arrays.asList(columns));
    }

    private static ADColumnMeta key(String name) {
        return new ADColumnMeta(name, name, null, 13, "ID", true, true, 10);
    }

    private static ADColumnMeta fk(String name) {
        return new ADColumnMeta(name, name, null, 19, "Table Direct", true, false, 10);
    }

    private static ADColumnMeta col(String name) {
        return new ADColumnMeta(name, name, null, 10, "String", false, false, 60);
    }
}
