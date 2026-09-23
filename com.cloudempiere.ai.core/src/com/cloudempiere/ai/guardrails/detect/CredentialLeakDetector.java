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
 * Detects credential leakage in LLM output: API keys, passwords, connection strings,
 * AWS credentials, private keys.
 */
public final class CredentialLeakDetector {

    private static final Pattern API_KEY_PATTERN = Pattern.compile(
        "(?i)(api[_-]?key|apikey|secret[_-]?key|access[_-]?token)[\\s]*[:=][\\s]*['\"]?([a-zA-Z0-9_-]{20,})['\"]?");

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "(?i)(password|passwd|pwd)[\\s]*[:=][\\s]*['\"]?([^\\s'\"]{4,})['\"]?");

    private static final Pattern CONNECTION_STRING_PATTERN = Pattern.compile(
        "(?i)(jdbc:|mongodb://|postgresql://|mysql://|redis://|amqp://)[^\\s]+");

    private static final Pattern AWS_CREDENTIAL_PATTERN = Pattern.compile(
        "(?i)(AKIA[0-9A-Z]{16}|aws[_-]?(access[_-]?key|secret)[\\s]*[:=])");

    private static final Pattern PRIVATE_KEY_PATTERN = Pattern.compile(
        "-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----");

    private CredentialLeakDetector() {
    }

    public static List<String> detect(String output) {
        List<String> types = new ArrayList<>();
        if (API_KEY_PATTERN.matcher(output).find()) {
            types.add("API Key");
        }
        if (PASSWORD_PATTERN.matcher(output).find()) {
            types.add("Password");
        }
        if (CONNECTION_STRING_PATTERN.matcher(output).find()) {
            types.add("Connection String");
        }
        if (AWS_CREDENTIAL_PATTERN.matcher(output).find()) {
            types.add("AWS Credential");
        }
        if (PRIVATE_KEY_PATTERN.matcher(output).find()) {
            types.add("Private Key");
        }
        return types;
    }
}
