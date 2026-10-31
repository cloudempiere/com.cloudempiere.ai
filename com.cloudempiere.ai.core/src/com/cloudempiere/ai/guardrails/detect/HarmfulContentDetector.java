/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2025 Cloudempiere                                            *
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
package com.cloudempiere.ai.guardrails.detect;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Detects harmful content in LLM output: code execution patterns and SQL injection
 * (the latter delegated to {@link SqlInjectionDetector}, shared with the input side).
 */
public final class HarmfulContentDetector {

    private static final Pattern CODE_EXECUTION_PATTERN = Pattern.compile(
        "(?i)(Runtime\\.exec|ProcessBuilder|exec\\(|system\\(|eval\\(|subprocess\\.)");

    private HarmfulContentDetector() {
    }

    public static List<String> detect(String output) {
        List<String> types = new ArrayList<>();
        if (CODE_EXECUTION_PATTERN.matcher(output).find()) {
            types.add("Code Execution");
        }
        if (SqlInjectionDetector.matches(output)) {
            types.add("Destructive SQL");
        }
        return types;
    }
}
