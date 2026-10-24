package com.cloudempiere.ai.test.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.cloudempiere.ai.database.ReadOnlySqlValidator;
import com.cloudempiere.ai.test.categories.UnitTest;

/**
 * The text check in front of the AI query executor (CLD-2355). It is the second layer; the first is the read-only
 * transaction. Each statement below was seen to pass the previous check, or is a plausible way around this one.
 */
@UnitTest
@DisplayName("ReadOnlySqlValidator")
class ReadOnlySqlValidatorTest {

    @Nested
    @DisplayName("accepts ordinary read queries")
    class Accepts {

        @ParameterizedTest(name = "[{index}] {0}")
        @ValueSource(strings = {
            "SELECT * FROM AD_User",
            "SELECT * FROM C_Order WHERE IsActive='Y' ORDER BY Created DESC",
            "SELECT Name, Created, CreatedBy, Updated, UpdatedBy FROM AD_User",
            "select count(*) from M_Storage s join M_Product p on s.M_Product_ID = p.M_Product_ID",
            "SELECT CASE WHEN QtyOnHand > 0 THEN 'in stock' ELSE 'out' END FROM M_Storage",
            "SELECT * FROM M_Product WHERE Name LIKE '%set%'",
            "SELECT * FROM M_Product WHERE Value = 'Insert' OR Name = 'Delete me'",
            "SELECT 'it''s fine; really' AS msg FROM Dual",
            "SELECT (SELECT MAX(Created) FROM AD_User) FROM Dual",
            "SELECT current_setting('server_version')",
            "SELECT * FROM AD_User;",
            "SELECT *\nFROM AD_User\nWHERE IsActive = 'Y'",
        })
        void ordinaryQueries(String sql) {
            assertThatCode(() -> ReadOnlySqlValidator.validate(sql)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("rejects what the previous text check let through")
    class RejectsPreviousGaps {

        @ParameterizedTest(name = "[{index}] {0}")
        @ValueSource(strings = {
            // SELECT ... INTO creates a table in PostgreSQL
            "SELECT * INTO newtable FROM AD_User",
            "SELECT\t*\tINTO\tnewtable\tFROM\tAD_User",
            "SELECT * INTO TEMP newtable FROM AD_User",
            // functions with effects that no read-only transaction stops
            "SELECT nextval('ad_sequence_no')",
            "SELECT setval('ad_sequence_no', 1)",
            "SELECT set_config('search_path', 'x', false)",
            "SELECT pg_terminate_backend(1234)",
            "SELECT pg_cancel_backend(1234)",
            "SELECT pg_sleep(100)",
            "SELECT pg_advisory_lock(1)",
            "SELECT lo_import('/etc/passwd')",
            "SELECT pg_read_file('/etc/passwd')",
            "SELECT dblink_exec('host=x', 'DROP TABLE t')",
            "SELECT DBMS_LOCK.SLEEP(10) FROM Dual",
            // schema-qualified and quoted names of the same functions
            "SELECT pg_catalog.pg_sleep(1)",
            "SELECT public.nextval('s')",
            "SELECT \"nextval\"('s')",
            "SELECT \"pg_terminate_backend\"(1)",
            "SELECT UTL_HTTP.REQUEST('http://x') FROM Dual",
            // row locks block other sessions
            "SELECT * FROM AD_User FOR SHARE",
            "SELECT * FROM AD_User FOR NO KEY UPDATE",
        })
        void gapsThatWereOpen(String sql) {
            assertThatThrownBy(() -> ReadOnlySqlValidator.validate(sql)).isInstanceOf(SecurityException.class);
        }
    }

    @Nested
    @DisplayName("rejects writes, transaction control and ways of hiding them")
    class RejectsWrites {

        @ParameterizedTest(name = "[{index}] {0}")
        @ValueSource(strings = {
            "INSERT INTO C_Order (Name) VALUES ('Test')",
            "UPDATE C_Order SET Name='Test'",
            "DELETE FROM C_Order",
            "DROP TABLE C_Order",
            "WITH d AS (DELETE FROM AD_User RETURNING *) SELECT * FROM d",
            "SELECT 1; COMMIT",
            "SELECT 1; SELECT 2",
            "SELECT 1;INSERT INTO x VALUES (1)",
            "SELECT 1 FROM Dual WHERE 1=1 -- hidden",
            "SELECT 1 /* hidden */ FROM Dual",
            // whitespace other than a blank between the words
            "SELECT x FROM t WHERE y IN (1)\tDELETE\tFROM t",
            "SELECT x FROM t\nINSERT\nINTO x VALUES (1)",
            // quoting forms the check does not parse
            "SELECT $$x$$",
            "SELECT $tag$x$tag$",
            "SELECT E'x\\'y'",
            // an unterminated quote would hide the rest
            "SELECT 'unterminated",
            "SELECT \"unterminated",
            // a single quote inside a double-quoted identifier must not hide the call that follows
            "SELECT \"a'\", nextval('s'), \"b'\" FROM t",
            // the transaction mode cannot be switched from inside the statement
            "SELECT set_config('transaction_read_only', 'off', true)",
            "SELECT 1; SET TRANSACTION READ WRITE",
        })
        void writesAndEscapes(String sql) {
            assertThatThrownBy(() -> ReadOnlySqlValidator.validate(sql)).isInstanceOf(SecurityException.class);
        }
    }

    @Nested
    @DisplayName("input checks")
    class Input {

        @Test
        void nullIsAnArgumentError() {
            assertThatThrownBy(() -> ReadOnlySqlValidator.validate(null)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void blankIsAnArgumentError() {
            assertThatThrownBy(() -> ReadOnlySqlValidator.validate("   \n\t ")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("the message names what was found, so the model can correct itself")
        void messagesAreSpecific() {
            assertThatThrownBy(() -> ReadOnlySqlValidator.validate("SELECT * INTO t FROM AD_User"))
                .hasMessageContaining("INTO");
            assertThatThrownBy(() -> ReadOnlySqlValidator.validate("SELECT pg_sleep(1)"))
                .hasMessageContaining("PG_SLEEP");
            assertThat(true).isTrue();
        }
    }
}
