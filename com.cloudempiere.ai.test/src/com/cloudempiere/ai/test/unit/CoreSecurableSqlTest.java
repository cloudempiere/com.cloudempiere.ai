/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2026 Cloudempiere                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package com.cloudempiere.ai.test.unit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.cloudempiere.ai.database.CoreSqlScope;
import com.cloudempiere.ai.test.categories.IntegrationTest;

/**
 * CLD-2546: the AI may run only SQL that core's MRole.addAccessSQL secures completely (one SELECT on one table),
 * decided by core's AccessSqlParser. The statements below are the bypasses reproduced on the dev server.
 *
 * <p>Integration test: core's AccessSqlParser looks up an OSGi service on load (org.cloudempiere.base.Core), so it
 * needs the OSGi runtime, not the plain unit-test class path.
 */
@IntegrationTest
public class CoreSecurableSqlTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
        "SELECT COUNT(*) FROM C_BPartner",
        "SELECT Name, Value FROM C_BPartner bp WHERE bp.IsActive='Y'",
        "SELECT DocumentNo FROM C_Order o WHERE o.C_BPartner_ID = 1048242 AND o.DocStatus IN ('CO','CL')",
        "SELECT * FROM M_Product WHERE Name = 'select me'",
    })
    public void oneTableIsAccepted(String sql) {
        assertThatCode(() -> CoreSqlScope.assertSecurableByCore(sql)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
        // reproduced: the joined table got no tenant filter (16 tenants instead of 2)
        "SELECT COUNT(DISTINCT u.AD_Client_ID) FROM C_BPartner bp JOIN C_BPartner u ON (u.C_BPartner_ID > 0) WHERE bp.C_BPartner_ID=1048242",
        "SELECT o.DocumentNo, bp.Name FROM C_Order o JOIN C_BPartner bp ON (bp.C_BPartner_ID = o.C_BPartner_ID)",
        "SELECT o.DocumentNo FROM C_Order o LEFT OUTER JOIN C_BPartner bp ON (bp.C_BPartner_ID = o.C_BPartner_ID)",
        "SELECT a.Name FROM C_BPartner a, AD_User b WHERE a.C_BPartner_ID = b.C_BPartner_ID",
        // lowercase / non-AD joined tables were not checked at all
        "SELECT bp.Name FROM C_BPartner bp JOIN ad_authorizationtoken t ON (t.AD_User_ID > 0)",
    })
    public void moreThanOneTableIsRejected(String sql) {
        assertThatThrownBy(() -> CoreSqlScope.assertSecurableByCore(sql))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining("one table");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
        // reproduced: the subquery read all tenants, and a table the role may not open
        "SELECT (SELECT COUNT(DISTINCT x.AD_Client_ID) FROM C_BPartner x) AS tenants FROM C_BPartner bp",
        "SELECT (SELECT COUNT(DISTINCT t.AD_Client_ID) FROM AD_AuthorizationToken t) FROM C_BPartner bp",
        "SELECT Name FROM C_BPartner WHERE C_BPartner_ID IN (SELECT C_BPartner_ID FROM C_Order)",
        "SELECT Name FROM C_BPartner bp WHERE EXISTS (SELECT 1 FROM AD_User u WHERE u.C_BPartner_ID = bp.C_BPartner_ID)",
        "SELECT * FROM (SELECT * FROM AD_User) u",
        "select name from c_bpartner where c_bpartner_id = (select min(c_bpartner_id) from c_bpartner)",
    })
    public void subqueriesAreRejected(String sql) {
        assertThatThrownBy(() -> CoreSqlScope.assertSecurableByCore(sql))
            .isInstanceOf(SecurityException.class);
    }

    @Test
    public void lowercaseKeywordsAreRejectedWithAHint() {
        // core's AccessSqlParser reads " FROM " in upper case only; lowercase table names are fine
        assertThatThrownBy(() -> CoreSqlScope.assertSecurableByCore("select count(*) from C_BPartner"))
            .isInstanceOf(SecurityException.class).hasMessageContaining("upper case");
        assertThatCode(() -> CoreSqlScope.assertSecurableByCore("SELECT COUNT(*) FROM c_bpartner"))
            .doesNotThrowAnyException();
    }

    @Test
    public void messagesTellTheModelWhatToDoInstead() {
        assertThatThrownBy(() -> CoreSqlScope.assertSecurableByCore(
            "SELECT Name FROM C_BPartner WHERE C_BPartner_ID IN (SELECT C_BPartner_ID FROM C_Order)"))
            .hasMessageContaining("Subqueries are not allowed").hasMessageContaining("second query");
    }
}
