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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.langchain4j.AIAccessDeniedException;
import com.cloudempiere.ai.test.categories.UnitTest;

/**
 * CLD-2541: the ADR-058 access decision that AIService enforces at every entry point.
 */
@UnitTest
public class AIProviderAccessDecisionTest {

    private final AtomicInteger grantLookups = new AtomicInteger();

    private BooleanSupplier grant(boolean exists) {
        return () -> {
            grantLookups.incrementAndGet();
            return exists;
        };
    }

    @Test
    public void noneIsDeniedEvenWithAGrant() {
        assertThat(MAIProvider.isAccessAllowed(MAIProvider.AIACCESSLEVEL_None, true, grant(true))).isFalse();
        assertThat(grantLookups).hasValue(0);
    }

    @Test
    public void missingAccessLevelIsDenied() {
        // No role, or AD_Role has no AIAccessLevel column (upstream core)
        assertThat(MAIProvider.isAccessAllowed(null, true, grant(true))).isFalse();
    }

    @Test
    public void allIsAllowedWithoutAGrantLookup() {
        assertThat(MAIProvider.isAccessAllowed(MAIProvider.AIACCESSLEVEL_All, true, grant(false))).isTrue();
        assertThat(grantLookups).hasValue(0);
    }

    @Test
    public void userRoleAccessNeedsAGrant() {
        assertThat(MAIProvider.isAccessAllowed(MAIProvider.AIACCESSLEVEL_UserRoleAccess, true, grant(true))).isTrue();
        assertThat(MAIProvider.isAccessAllowed(MAIProvider.AIACCESSLEVEL_UserRoleAccess, true, grant(false))).isFalse();
        assertThat(grantLookups).hasValue(2);
    }

    @Test
    public void unusableProviderIsDeniedAtEveryLevel() {
        // Inactive, or owned by another tenant
        for (String level : new String[] { MAIProvider.AIACCESSLEVEL_All, MAIProvider.AIACCESSLEVEL_UserRoleAccess,
                MAIProvider.AIACCESSLEVEL_None }) {
            assertThat(MAIProvider.isAccessAllowed(level, false, grant(true))).as(level).isFalse();
        }
        assertThat(grantLookups).hasValue(0);
    }

    @Test
    public void unknownAccessLevelFailsClosed() {
        assertThat(MAIProvider.isAccessAllowed("X", true, grant(true))).isFalse();
    }

    @Test
    public void accessDeniedIsASecurityException() {
        assertThat(new AIAccessDeniedException())
            .isInstanceOf(SecurityException.class)
            .hasMessage(AIAccessDeniedException.MESSAGE);
    }
}
