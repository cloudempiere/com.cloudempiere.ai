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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects and masks PII: SSN, credit card, Tax ID, email, phone.
 */
public final class PiiDetector {

    /** US Social Security Number: XXX-XX-XXXX (requires separators to avoid false positives) */
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}[-\\s]\\d{2}[-\\s]\\d{4}\\b");

    /** Credit Card: 13-19 digits with optional separators - see {@link #isLuhnValid} for why
     * a length match alone isn't treated as a hit. */
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,19}\\b");

    /** Email address */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b", Pattern.CASE_INSENSITIVE);

    /** Phone number (various formats) */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "\\b(?:\\+?1[-\\s.]?)?\\(?\\d{3}\\)?[-\\s.]?\\d{3}[-\\s.]?\\d{4}\\b");

    /** Tax ID / EIN (US format: XX-XXXXXXX with required separator) */
    private static final Pattern TAX_ID_PATTERN = Pattern.compile("\\b\\d{2}[-\\s]\\d{7}\\b");

    // Bank account detection stays disabled - see ADR-057. The pattern was too broad
    // (matched order IDs, invoice numbers) and language-dependent keyword matching
    // doesn't work for a multi-language ERP. Needs an IBAN prefix check or ML
    // classification, not a length-only regex.

    public static final class Result {
        public final boolean hasPII;
        public final List<String> types;
        public final String maskedText;

        Result(boolean hasPII, List<String> types, String maskedText) {
            this.hasPII = hasPII;
            this.types = types;
            this.maskedText = maskedText;
        }
    }

    private PiiDetector() {
    }

    public static Result detectAndMask(String input, String maskChar) {
        List<String> types = new ArrayList<>();
        String result = input;

        if (SSN_PATTERN.matcher(result).find()) {
            types.add("SSN");
            result = SSN_PATTERN.matcher(result).replaceAll(maskChar.repeat(11));
        }

        if (hasLuhnValidCreditCard(result)) {
            types.add("Credit Card");
            result = maskCreditCard(result, maskChar);
        }

        if (TAX_ID_PATTERN.matcher(result).find()) {
            types.add("Tax ID");
            result = TAX_ID_PATTERN.matcher(result).replaceAll(maskChar.repeat(10));
        }

        if (EMAIL_PATTERN.matcher(result).find()) {
            types.add("Email");
            result = maskEmail(result, maskChar);
        }

        if (PHONE_PATTERN.matcher(result).find()) {
            types.add("Phone");
            result = maskPhone(result, maskChar);
        }

        return new Result(!types.isEmpty(), types, result);
    }

    /**
     * True if the text contains a 13-19 digit candidate (with optional grouping
     * separators) that also satisfies the Luhn checksum every real card number
     * satisfies. Matching on length alone (the old behavior) false-positive-masked
     * ordinary 13-19 digit ERP identifiers - invoice numbers, order numbers - as
     * credit cards, the exact false-positive shape already diagnosed and disabled
     * for the bank-account pattern above. A bare business identifier of the same
     * length essentially never satisfies Luhn by chance (~1-in-10), so this filters
     * those out while still catching real card numbers, including ungrouped ones.
     */
    private static boolean hasLuhnValidCreditCard(String text) {
        Matcher m = CREDIT_CARD_PATTERN.matcher(text);
        while (m.find()) {
            if (isLuhnValid(m.group().replaceAll("[ -]", ""))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLuhnValid(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    private static String maskCreditCard(String input, String maskChar) {
        Matcher m = CREDIT_CARD_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String cc = m.group().replaceAll("[ -]", "");
            if (!isLuhnValid(cc)) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
                continue;
            }
            String masked = maskChar.repeat(cc.length() - 4) + cc.substring(cc.length() - 4);
            m.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String maskEmail(String input, String maskChar) {
        Matcher m = EMAIL_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String email = m.group();
            int atIndex = email.indexOf('@');
            String masked = maskChar.repeat(atIndex) + email.substring(atIndex);
            m.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static String maskPhone(String input, String maskChar) {
        Matcher m = PHONE_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String phone = m.group().replaceAll("[^\\d]", "");
            String masked = maskChar.repeat(Math.max(0, phone.length() - 4)) +
                           phone.substring(Math.max(0, phone.length() - 4));
            m.appendReplacement(sb, Matcher.quoteReplacement(masked));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
