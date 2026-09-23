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

import java.util.regex.Pattern;

/**
 * Detects prompt-injection attempts and system-prompt-extraction attempts in user input.
 */
public final class InjectionDetector {

    private static final Pattern INJECTION_PATTERN = Pattern.compile(
        "(?i)\\b(" +
        "ignore\\s+(previous|all|above|prior)\\s+(instructions?|rules?|commands?)" +
        "|disregard\\s+(previous|all|above|prior)\\s+(instructions?|rules?|commands?)" +
        "|forget\\s+(everything|all|your)\\s+(instructions?|rules?|training)" +
        "|you\\s+are\\s+now\\s+a" +
        "|pretend\\s+(to\\s+be|you\\s+are)" +
        "|act\\s+as\\s+(if|though)" +
        "|override\\s+(previous|system|your)" +
        "|bypass\\s+(security|restrictions?|rules?)" +
        "|jailbreak" +
        "|do\\s+anything\\s+now" +
        "|ignore\\s+safety" +
        "|new\\s+instructions?" +
        ")\\b"
    );

    private static final Pattern SYSTEM_PROMPT_EXTRACTION = Pattern.compile(
        "(?i)\\b(" +
        "(show|reveal|tell|give|display|print|output)\\s+(me\\s+)?(your|the|system)\\s+(prompt|instructions?|rules?)" +
        "|what\\s+(are|is)\\s+your\\s+(system\\s+)?(prompt|instructions?|rules?)" +
        "|repeat\\s+(your\\s+)?(system\\s+)?(prompt|instructions?)" +
        ")\\b"
    );

    private InjectionDetector() {
    }

    public static boolean isPromptInjection(String text) {
        return text != null && INJECTION_PATTERN.matcher(text).find();
    }

    public static boolean isSystemPromptExtraction(String text) {
        return text != null && SYSTEM_PROMPT_EXTRACTION.matcher(text).find();
    }
}
