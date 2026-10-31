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
 * Detects phrases in LLM output that indicate uncertainty or unverified real-time
 * claims - not blocked, just flagged so a disclaimer can be added.
 */
public final class HallucinationDetector {

    private static final Pattern HALLUCINATION_PATTERN = Pattern.compile(
        "(?i)\\b(" +
        "I cannot verify" +
        "|I'm not (certain|sure) (if|whether)" +
        "|I don't have (access|information) (about|on)" +
        "|as of my (knowledge|training) cutoff" +
        "|I cannot (access|retrieve) (real-?time|current)" +
        "|based on (my|the) (training|knowledge)" +
        "|I (assume|presume|believe|think) (that|this)" +
        "|this (may|might|could) (be|not be) accurate" +
        ")\\b"
    );

    private static final Pattern UNVERIFIED_CLAIM_PATTERN = Pattern.compile(
        "(?i)\\b(" +
        "current(ly)?\\s+(is|are|has|have)" +
        "|as of (today|now|this moment)" +
        "|latest\\s+(data|figures|numbers)" +
        "|real-?time\\s+(data|information)" +
        ")\\b"
    );

    private HallucinationDetector() {
    }

    public static List<String> detect(String output) {
        List<String> indicators = new ArrayList<>();
        if (HALLUCINATION_PATTERN.matcher(output).find()) {
            indicators.add("Uncertainty indicator");
        }
        if (UNVERIFIED_CLAIM_PATTERN.matcher(output).find()) {
            indicators.add("Unverified real-time claim");
        }
        return indicators;
    }
}
