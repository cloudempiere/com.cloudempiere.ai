package com.cloudempiere.ai.test.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.database.dto.ADColumnMeta;
import com.cloudempiere.ai.database.dto.ADTableMeta;
import com.cloudempiere.ai.provider.langchain4j.ERPTools;
import com.cloudempiere.ai.test.categories.UnitTest;

/**
 * The schema the model sees names, for every reference, the table it points to and that table's name columns, so the
 * model returns names with one JOIN instead of looking records up afterwards (each lookup is a model round trip).
 * Before, only TableDirect columns counted as references: C_Order.C_BPartner_ID (Search) looked like a plain column.
 * Relations follow clde-hub ADContextService: AD_Ref_Table for Table/Search references, else the &lt;Table&gt;_ID name.
 */
@UnitTest
@DisplayName("Foreign key hints for the AI")
class ForeignKeyHintTest {

    private static final int TABLE_DIRECT = 19;
    private static final int TABLE = 18;
    private static final int SEARCH = 30;
    private static final int STRING = 10;

    private final Map<String, ADTableMeta> tables = new HashMap<>();
    private final Function<String, ADTableMeta> lookup = name -> tables.get(name.toUpperCase());

    ForeignKeyHintTest() {
        tables.put("C_BPARTNER", table("C_BPartner", column("C_BPartner_ID", 13, true, null, false),
            column("Value", STRING, false, null, true), column("Name", STRING, false, null, true)));
        tables.put("AD_USER", table("AD_User", column("AD_User_ID", 13, true, null, false),
            column("Name", STRING, false, null, true)));
        tables.put("C_CURRENCY", table("C_Currency", column("C_Currency_ID", 13, true, null, false),
            column("ISO_Code", STRING, false, null, true)));
    }

    @Test
    @DisplayName("a Search column without reference value points to the table named like it")
    void searchColumnByName() {
        ADColumnMeta bpartner = column("C_BPartner_ID", SEARCH, false, null, false);

        assertThat(bpartner.getForeignTable()).isEqualTo("C_BPartner");
        assertThat(ERPTools.foreignKeyLabel(bpartner, lookup)).isEqualTo("C_BPartner_ID -> C_BPartner (name: Value, Name)");
    }

    @Test
    @DisplayName("a Table column points to the table its AD_Ref_Table names")
    void tableColumnByReference() {
        ADColumnMeta salesRep = column("SalesRep_ID", TABLE, false, "AD_User", false);

        assertThat(salesRep.getForeignTable()).isEqualTo("AD_User");
        assertThat(ERPTools.foreignKeyLabel(salesRep, lookup)).isEqualTo("SalesRep_ID -> AD_User (name: Name)");
    }

    @Test
    @DisplayName("a TableDirect column still points to the table named like it")
    void tableDirectColumn() {
        ADColumnMeta currency = column("C_Currency_ID", TABLE_DIRECT, false, null, false);

        assertThat(ERPTools.foreignKeyLabel(currency, lookup)).isEqualTo("C_Currency_ID -> C_Currency (name: ISO_Code)");
    }

    @Test
    @DisplayName("keys, plain columns and unknown targets get no name hint")
    void noHint() {
        assertThat(column("C_Order_ID", TABLE_DIRECT, true, null, false).getForeignTable()).isNull();
        assertThat(column("DocumentNo", STRING, false, null, true).getForeignTable()).isNull();
        assertThat(ERPTools.foreignKeyLabel(column("DocumentNo", STRING, false, null, true), lookup)).isEqualTo("DocumentNo");
        assertThat(ERPTools.foreignKeyLabel(column("X_Unknown_ID", SEARCH, false, null, false), lookup))
            .isEqualTo("X_Unknown_ID -> X_Unknown");
    }

    @Test
    @DisplayName("a table's name columns are its identifier columns in order")
    void identifierColumns() {
        assertThat(tables.get("C_BPARTNER").getIdentifierColumns()).containsExactly("Value", "Name");
    }

    // ------------------------------------------------------------------------

    private static ADTableMeta table(String name, ADColumnMeta... columns) {
        return new ADTableMeta(0, name, name, null, "3", false, Arrays.asList(columns));
    }

    private static ADColumnMeta column(String name, int reference, boolean key, String referencedTable, boolean identifier) {
        return new ADColumnMeta(name, name, null, reference, null, false, key, 10, referencedTable, identifier);
    }
}
