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
package com.cloudempiere.ai.guardrails;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.util.CLogger;

import com.cloudempiere.ai.guardrails.dto.GuardResult;

/**
 * Input Guard for AI safety (ADR-014).
 *
 * <p>Pre-processes user input to detect and handle:
 * <ul>
 *   <li>PII (Personally Identifiable Information) - SSN, credit cards, etc.</li>
 *   <li>Prompt injection attempts - "ignore", "disregard", etc.</li>
 *   <li>SQL injection patterns - even for natural language inputs</li>
 *   <li>Potentially harmful commands</li>
 * </ul>
 *
 * <p>Actions:
 * <ul>
 *   <li>PASS - Input is safe, proceed</li>
 *   <li>MASK - PII detected and masked, proceed with masked input</li>
 *   <li>BLOCK - Dangerous input detected, reject request</li>
 * </ul>
 *
 * <p><b>Tags:</b> #guardrails #pii #input-validation #security #injection-prevention
 *
 * @author Cloudempiere AI Team
 * @version 1.0.0
 * @since v0.11.0
 * @see OutputGuard
 * @see ExecutionGuard
 * @see com.cloudempiere.ai.guardrails.dto.GuardResult
 */
public class InputGuard {

    private static final CLogger log = CLogger.getCLogger(InputGuard.class);

    // ========================================================================
    // PII Detection Patterns
    // ========================================================================

    /** US Social Security Number: XXX-XX-XXXX (requires separators to avoid false positives) */
    private static final Pattern SSN_PATTERN = Pattern.compile(
        "\\b\\d{3}[-\\s]\\d{2}[-\\s]\\d{4}\\b"
    );

    /** Credit Card: 13-19 digits with optional separators */
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile(
        "\\b(?:\\d[ -]*?){13,19}\\b"
    );

    /** Email address */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b",
        Pattern.CASE_INSENSITIVE
    );

    /** Phone number (various formats) */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "\\b(?:\\+?1[-\\s.]?)?\\(?\\d{3}\\)?[-\\s.]?\\d{3}[-\\s.]?\\d{4}\\b"
    );

    /** Tax ID / EIN (US format: XX-XXXXXXX with required separator) */
    private static final Pattern TAX_ID_PATTERN = Pattern.compile(
        "\\b\\d{2}[-\\s]\\d{7}\\b"
    );

    /** Bank account number (8-17 digits) */
    // DISABLED: Pattern too broad - matches order IDs, invoice numbers, etc.
    // Causes false positives for legitimate business identifiers.
    // Language-dependent keyword matching doesn't work for multi-language ERP.
    // TODO: Re-enable with proper context-aware detection (see ADR-057)
    //       Need IBAN pattern (country code prefix) or ML-based classification.
    // private static final Pattern BANK_ACCOUNT_PATTERN = Pattern.compile(
    //     "\\b\\d{8,17}\\b"
    // );

    // ========================================================================
    // Prompt Injection Patterns
    // ========================================================================

    /** Common prompt injection phrases */
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

    /** System prompt extraction attempts */
    private static final Pattern SYSTEM_PROMPT_EXTRACTION = Pattern.compile(
        "(?i)\\b(" +
        "(show|reveal|tell|give|display|print|output)\\s+(me\\s+)?(your|the|system)\\s+(prompt|instructions?|rules?)" +
        "|what\\s+(are|is)\\s+your\\s+(system\\s+)?(prompt|instructions?|rules?)" +
        "|repeat\\s+(your\\s+)?(system\\s+)?(prompt|instructions?)" +
        ")\\b"
    );

    // ========================================================================
    // SQL Injection Patterns (for natural language that might form queries)
    // ========================================================================

    /** SQL injection patterns */
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "(?i)(" +
        "'\\s*OR\\s+'?\\d*'?\\s*=\\s*'?\\d*" +
        "|'\\s*OR\\s+'[^']*'\\s*=\\s*'[^']*'" +
        "|;\\s*(DROP|DELETE|TRUNCATE|UPDATE|INSERT|ALTER)\\s+" +
        "|--\\s*$" +
        "|/\\*.*\\*/" +
        "|UNION\\s+(ALL\\s+)?SELECT" +
        "|\\bEXEC(UTE)?\\s*\\(" +
        "|\\bxp_\\w+" +
        ")"
    );

    // ========================================================================
    // Configuration
    // ========================================================================

    /** Whether to mask PII or block entirely */
    private boolean maskPII = true;

    /** Whether to block on injection attempts */
    private boolean blockInjection = true;

    /** PII masking character */
    private String maskChar = "*";

    /**
     * Create input guard with default configuration.
     */
    public InputGuard() {
    }

    /**
     * Create input guard with custom configuration.
     *
     * @param maskPII true to mask PII, false to block
     * @param blockInjection true to block injection attempts
     */
    public InputGuard(boolean maskPII, boolean blockInjection) {
        this.maskPII = maskPII;
        this.blockInjection = blockInjection;
    }

    /**
     * Validate and process input.
     *
     * @param input User input to validate
     * @return Guard result with action and processed input
     */
    public GuardResult validate(String input) {
        if (input == null || input.isEmpty()) {
            return GuardResult.pass(input);
        }

        List<String> violations = new ArrayList<>();
        String processedInput = input;

        // 1. Check for prompt injection
        if (containsInjection(input)) {
            violations.add("Prompt injection detected");
            if (blockInjection) {
                log.warning("Blocked prompt injection attempt: " + summarize(input));
                return GuardResult.block("Input contains potentially harmful instructions",
                    violations, "INJECTION");
            }
        }

        // 2. Check for system prompt extraction
        if (containsSystemPromptExtraction(input)) {
            violations.add("System prompt extraction attempt");
            log.warning("Blocked system prompt extraction: " + summarize(input));
            return GuardResult.block("Input appears to be attempting to extract system instructions",
                violations, "EXTRACTION");
        }

        // 3. Check for SQL injection patterns
        if (containsSqlInjection(input)) {
            violations.add("SQL injection pattern detected");
            log.warning("Blocked SQL injection pattern: " + summarize(input));
            return GuardResult.block("Input contains potentially harmful SQL patterns",
                violations, "SQL_INJECTION");
        }

        // 4. Detect and handle PII
        PIIDetectionResult piiResult = detectPII(input);
        if (piiResult.hasPII) {
            violations.addAll(piiResult.types);

            if (maskPII) {
                processedInput = piiResult.maskedInput;
                log.fine("Masked PII in input: " + piiResult.types);
                return GuardResult.mask(processedInput, violations);
            } else {
                log.warning("Blocked input with PII: " + piiResult.types);
                return GuardResult.block("Input contains sensitive personal information",
                    violations, "PII");
            }
        }

        // All checks passed
        return GuardResult.pass(processedInput);
    }

    /**
     * Check if input contains prompt injection patterns.
     */
    private boolean containsInjection(String input) {
        return INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Check if input attempts to extract system prompt.
     */
    private boolean containsSystemPromptExtraction(String input) {
        return SYSTEM_PROMPT_EXTRACTION.matcher(input).find();
    }

    /**
     * Check if input contains SQL injection patterns.
     */
    private boolean containsSqlInjection(String input) {
        return SQL_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Detect PII in input and optionally mask it.
     */
    private PIIDetectionResult detectPII(String input) {
        List<String> types = new ArrayList<>();
        String result = input;

        // Check and mask each PII type
        if (SSN_PATTERN.matcher(result).find()) {
            types.add("SSN");
            result = SSN_PATTERN.matcher(result).replaceAll(maskChar.repeat(11));
        }

        if (CREDIT_CARD_PATTERN.matcher(result).find()) {
            types.add("Credit Card");
            result = maskCreditCard(result);
        }

        if (TAX_ID_PATTERN.matcher(result).find()) {
            types.add("Tax ID");
            result = TAX_ID_PATTERN.matcher(result).replaceAll(maskChar.repeat(10));
        }

        // DISABLED: Bank account detection temporarily disabled (see ADR-057)
        // Pattern was too broad and caused false positives with business IDs.
        // if (BANK_ACCOUNT_PATTERN.matcher(result).find()) {
        //     types.add("Bank Account");
        //     result = BANK_ACCOUNT_PATTERN.matcher(result).replaceAll(maskChar.repeat(12));
        // }

        // Email and phone are lower risk - mask but don't block
        if (EMAIL_PATTERN.matcher(result).find()) {
            types.add("Email");
            result = maskEmail(result);
        }

        if (PHONE_PATTERN.matcher(result).find()) {
            types.add("Phone");
            result = maskPhone(result);
        }

        return new PIIDetectionResult(!types.isEmpty(), types, result);
    }

    /**
     * Mask credit card, keeping last 4 digits.
     */
    private String maskCreditCard(String input) {
        Matcher m = CREDIT_CARD_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String cc = m.group().replaceAll("[ -]", "");
            String masked = maskChar.repeat(cc.length() - 4) + cc.substring(cc.length() - 4);
            m.appendReplacement(sb, masked);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Mask email address, keeping domain.
     */
    private String maskEmail(String input) {
        Matcher m = EMAIL_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String email = m.group();
            int atIndex = email.indexOf('@');
            String masked = maskChar.repeat(atIndex) + email.substring(atIndex);
            m.appendReplacement(sb, masked);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Mask phone number, keeping last 4 digits.
     */
    private String maskPhone(String input) {
        Matcher m = PHONE_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String phone = m.group().replaceAll("[^\\d]", "");
            String masked = maskChar.repeat(Math.max(0, phone.length() - 4)) +
                           phone.substring(Math.max(0, phone.length() - 4));
            m.appendReplacement(sb, masked);
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Summarize input for logging (truncate if too long).
     */
    private String summarize(String input) {
        if (input.length() <= 100) {
            return input;
        }
        return input.substring(0, 100) + "...";
    }

    // ========================================================================
    // Configuration setters
    // ========================================================================

    public void setMaskPII(boolean maskPII) {
        this.maskPII = maskPII;
    }

    public void setBlockInjection(boolean blockInjection) {
        this.blockInjection = blockInjection;
    }

    public void setMaskChar(String maskChar) {
        this.maskChar = maskChar;
    }

    // ========================================================================
    // Inner classes
    // ========================================================================

    /**
     * Result of PII detection.
     */
    private static class PIIDetectionResult {
        final boolean hasPII;
        final List<String> types;
        final String maskedInput;

        PIIDetectionResult(boolean hasPII, List<String> types, String maskedInput) {
            this.hasPII = hasPII;
            this.types = types;
            this.maskedInput = maskedInput;
        }
    }
}
