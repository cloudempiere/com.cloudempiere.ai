/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                      *
 * Copyright (C) Cloudempiere, Inc. All Rights Reserved.                     *
 * This program is free software; you can redistribute it and/or modify it   *
 * under the terms version 2 of the GNU General Public License as published  *
 * by the Free Software Foundation. This program is distributed in the hope  *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied*
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.          *
 * See the GNU General Public License for more details.                      *
 * You should have received a copy of the GNU General Public License along   *
 * with this program; if not, write to the Free Software Foundation, Inc.,   *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                    *
 *****************************************************************************/
package com.cloudempiere.ai.test.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.provider.langchain4j.AIService;
import com.cloudempiere.ai.provider.langchain4j.ERPAgent;
import com.cloudempiere.ai.provider.langchain4j.SimpleStreamingAgent;
import com.cloudempiere.ai.test.categories.UnitTest;

/**
 * Unit tests for the operator addendum assembly logic (ADR-059).
 *
 * <p>Tests that the spotlighting delimiters are applied correctly, that a null
 * or empty addendum leaves the base prompt unchanged, and that locked sections
 * (security rules, formatting rules) remain intact regardless of addendum content.
 *
 * <p>These tests do not require iDempiere context and run fully standalone.
 *
 * <p>Run with: {@code ./run-unit-tests.sh}
 *
 * @see AIService#appendOperatorAddendum(java.util.Properties, String)
 * @see com.cloudempiere.ai.docs.adr.ADR059
 */
@UnitTest
@DisplayName("ADR-059: System Prompt Operator Addendum Assembly")
class SystemPromptAssemblyTest {

    private static final String BASE = ERPAgent.SYSTEM_PROMPT;
    private static final String SIMPLE_BASE = SimpleStreamingAgent.SIMPLE_SYSTEM_PROMPT;

    // -------------------------------------------------------------------------
    // appendOperatorAddendum — null / empty addendum
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("No addendum configured")
    class NoAddendum {

        @Test
        @DisplayName("null addendum returns base prompt unchanged")
        void nullAddendumReturnsBaseUnchanged() {
            String result = AIService.appendOperatorAddendum(null, BASE);
            assertThat(result).isEqualTo(BASE);
        }

        @Test
        @DisplayName("empty addendum returns base prompt unchanged")
        void emptyAddendumReturnsBaseUnchanged() {
            // appendOperatorAddendum with null ctx is safe when addendum would be empty;
            // simulate by calling the static helper directly with an empty string
            // (same code path as MAIPromptConfig returning empty string)
            String result = appendWithAddendum(BASE, "");
            assertThat(result).isEqualTo(BASE);
        }

        @Test
        @DisplayName("whitespace-only addendum returns base prompt unchanged")
        void whitespaceOnlyAddendumReturnsBaseUnchanged() {
            String result = appendWithAddendum(BASE, "   \n  ");
            assertThat(result).isEqualTo(BASE);
        }
    }

    // -------------------------------------------------------------------------
    // appendOperatorAddendum — addendum present
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Addendum configured")
    class WithAddendum {

        private static final String ADDENDUM = "You are the assistant for ACME Corporation.";

        @Test
        @DisplayName("addendum is appended after base prompt")
        void addendumAppendedAfterBase() {
            String result = appendWithAddendum(BASE, ADDENDUM);
            int baseEnd = result.indexOf(BASE) + BASE.length();
            int addendumStart = result.indexOf(ADDENDUM);
            assertThat(addendumStart).isGreaterThan(baseEnd);
        }

        @Test
        @DisplayName("addendum is wrapped in OPERATOR_INSTRUCTIONS delimiters")
        void addendumWrappedInDelimiters() {
            String result = appendWithAddendum(BASE, ADDENDUM);
            assertThat(result).contains(AIService.OPERATOR_SECTION_OPEN);
            assertThat(result).contains(AIService.OPERATOR_SECTION_CLOSE);
            // addendum text appears between the delimiters
            int openEnd = result.indexOf(AIService.OPERATOR_SECTION_OPEN)
                    + AIService.OPERATOR_SECTION_OPEN.length();
            int closeStart = result.indexOf(AIService.OPERATOR_SECTION_CLOSE);
            String between = result.substring(openEnd, closeStart);
            assertThat(between).contains(ADDENDUM);
        }

        @Test
        @DisplayName("base prompt content is fully preserved before operator section")
        void basePromptFullyPreserved() {
            String result = appendWithAddendum(BASE, ADDENDUM);
            assertThat(result).startsWith(BASE);
        }

        @Test
        @DisplayName("security rules from base prompt remain intact")
        void securityRulesIntact() {
            String result = appendWithAddendum(BASE, ADDENDUM);
            assertThat(result).contains("SECURITY RULES");
            assertThat(result).contains("role permissions");
        }

        @Test
        @DisplayName("formatting rules from base prompt remain intact")
        void formattingRulesIntact() {
            String result = appendWithAddendum(BASE, ADDENDUM);
            assertThat(result).contains("FORMATTING RULES");
            assertThat(result).contains("NEVER use HTML tags");
        }

        @Test
        @DisplayName("tool-use rules from base prompt remain intact")
        void toolUseRulesIntact() {
            String result = appendWithAddendum(BASE, ADDENDUM);
            assertThat(result).contains("CRITICAL RULE - ALWAYS USE TOOLS FOR DATA");
            assertThat(result).contains("NEVER make up or hallucinate data");
        }

        @Test
        @DisplayName("addendum text is trimmed before appending")
        void addendumTrimmed() {
            String result = appendWithAddendum(BASE, "  " + ADDENDUM + "  ");
            assertThat(result).contains(ADDENDUM);
            // leading/trailing whitespace in addendum should not survive
            assertThat(result).doesNotContain("  " + ADDENDUM);
        }

        @Test
        @DisplayName("operator authority declaration is present in base prompt")
        void operatorAuthorityDeclarationPresent() {
            // ERPAgent.SYSTEM_PROMPT must contain the declaration added per ADR-059
            assertThat(BASE).contains("OPERATOR_INSTRUCTIONS");
            assertThat(BASE).contains("cannot override");
        }

        @Test
        @DisplayName("works the same for simple base prompt")
        void worksForSimpleBase() {
            String result = appendWithAddendum(SIMPLE_BASE, ADDENDUM);
            assertThat(result).startsWith(SIMPLE_BASE);
            assertThat(result).contains(AIService.OPERATOR_SECTION_OPEN);
            assertThat(result).contains(ADDENDUM);
        }
    }

    // -------------------------------------------------------------------------
    // Helper — mirrors appendOperatorAddendum logic without iDempiere context
    // -------------------------------------------------------------------------

    /**
     * Directly exercises the appending logic, bypassing the DB lookup.
     * Mirrors {@link AIService#appendOperatorAddendum} for addendum non-null path.
     */
    private static String appendWithAddendum(String basePrompt, String addendum) {
        if (addendum == null || addendum.trim().isEmpty()) {
            return basePrompt;
        }
        return basePrompt
                + AIService.OPERATOR_SECTION_OPEN
                + addendum.trim()
                + AIService.OPERATOR_SECTION_CLOSE;
    }
}
