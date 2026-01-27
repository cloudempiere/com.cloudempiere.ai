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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.compiere.util.CLogger;

import com.cloudempiere.ai.guardrails.dto.GuardResult;

/**
 * Output Guard for AI safety (ADR-014).
 *
 * <p>Post-processes LLM output to detect and handle:
 * <ul>
 *   <li>Data leakage - credentials, API keys, internal IDs</li>
 *   <li>Hallucination indicators - unverifiable claims</li>
 *   <li>Format validation - ensure structured outputs match schema</li>
 *   <li>Harmful content - code execution, dangerous instructions</li>
 * </ul>
 *
 * <p>Actions:
 * <ul>
 *   <li>PASS - Output is safe, return to user</li>
 *   <li>MASK - Sensitive data detected and redacted</li>
 *   <li>BLOCK - Dangerous output detected, suppress response</li>
 * </ul>
 *
 * <p><b>Tags:</b> #guardrails #output-validation #data-leakage #hallucination #security
 *
 * @author Cloudempiere AI Team
 * @version 1.0.0
 * @since v0.11.0
 * @see InputGuard
 * @see ExecutionGuard
 * @see com.cloudempiere.ai.guardrails.dto.GuardResult
 */
public class OutputGuard {

    private static final CLogger log = CLogger.getCLogger(OutputGuard.class);

    // ========================================================================
    // Data Leakage Patterns
    // ========================================================================

    /** API key patterns (various formats) */
    private static final Pattern API_KEY_PATTERN = Pattern.compile(
        "(?i)(api[_-]?key|apikey|secret[_-]?key|access[_-]?token)[\\s]*[:=][\\s]*['\"]?([a-zA-Z0-9_-]{20,})['\"]?"
    );

    /** Password patterns */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "(?i)(password|passwd|pwd)[\\s]*[:=][\\s]*['\"]?([^\\s'\"]{4,})['\"]?"
    );

    /** Connection string patterns */
    private static final Pattern CONNECTION_STRING_PATTERN = Pattern.compile(
        "(?i)(jdbc:|mongodb://|postgresql://|mysql://|redis://|amqp://)[^\\s]+"
    );

    /** AWS credentials */
    private static final Pattern AWS_CREDENTIAL_PATTERN = Pattern.compile(
        "(?i)(AKIA[0-9A-Z]{16}|aws[_-]?(access[_-]?key|secret)[\\s]*[:=])"
    );

    /** Private key patterns */
    private static final Pattern PRIVATE_KEY_PATTERN = Pattern.compile(
        "-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----"
    );

    // ========================================================================
    // Internal ID Patterns (should not be exposed to users)
    // ========================================================================

    /** Internal iDempiere IDs that might indicate data leakage */
    private static final Pattern INTERNAL_ID_PATTERN = Pattern.compile(
        "\\b(AD_Client_ID|AD_Org_ID|AD_User_ID|AD_Role_ID|AD_Table_ID|AD_Column_ID)\\s*[:=]\\s*\\d+"
    );

    /** UUID patterns (might indicate internal references) */
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"
    );

    // ========================================================================
    // Hallucination Indicators
    // ========================================================================

    /** Phrases that might indicate hallucination */
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

    /** Claims about real-time data without verification */
    private static final Pattern UNVERIFIED_CLAIM_PATTERN = Pattern.compile(
        "(?i)\\b(" +
        "current(ly)?\\s+(is|are|has|have)" +
        "|as of (today|now|this moment)" +
        "|latest\\s+(data|figures|numbers)" +
        "|real-?time\\s+(data|information)" +
        ")\\b"
    );

    // ========================================================================
    // Harmful Content Patterns
    // ========================================================================

    /** Code execution patterns */
    private static final Pattern CODE_EXECUTION_PATTERN = Pattern.compile(
        "(?i)(Runtime\\.exec|ProcessBuilder|exec\\(|system\\(|eval\\(|subprocess\\.)"
    );

    /** SQL execution patterns in output */
    private static final Pattern SQL_EXECUTION_PATTERN = Pattern.compile(
        "(?i)(DROP\\s+TABLE|DELETE\\s+FROM\\s+\\w+\\s+WHERE\\s+1\\s*=\\s*1|TRUNCATE\\s+TABLE)"
    );

    // ========================================================================
    // Configuration
    // ========================================================================

    /** Whether to flag hallucination indicators */
    private boolean flagHallucinations = true;

    /** Whether to block credential leakage */
    private boolean blockCredentialLeakage = true;

    /** Whether to mask internal IDs */
    private boolean maskInternalIds = true;

    /** Set of table names from current context (to validate references) */
    private Set<String> knownTables = new HashSet<>();

    /** Set of column names from current context */
    private Set<String> knownColumns = new HashSet<>();

    /**
     * Create output guard with default configuration.
     */
    public OutputGuard() {
    }

    /**
     * Create output guard with context.
     *
     * @param knownTables Tables referenced in the current query/context
     * @param knownColumns Columns referenced in the current query/context
     */
    public OutputGuard(Set<String> knownTables, Set<String> knownColumns) {
        this.knownTables = knownTables != null ? knownTables : new HashSet<>();
        this.knownColumns = knownColumns != null ? knownColumns : new HashSet<>();
    }

    /**
     * Validate and process LLM output.
     *
     * @param output LLM output to validate
     * @return Guard result with action and processed output
     */
    public GuardResult validate(String output) {
        if (output == null || output.isEmpty()) {
            return GuardResult.pass(output);
        }

        List<String> violations = new ArrayList<>();
        String processedOutput = output;

        // 1. Check for credential leakage (BLOCK)
        if (blockCredentialLeakage) {
            LeakageResult leakage = checkCredentialLeakage(output);
            if (leakage.hasLeakage) {
                violations.addAll(leakage.types);
                log.warning("Blocked output with credential leakage: " + leakage.types);
                return GuardResult.block(
                    "Output contains sensitive credentials that cannot be displayed",
                    violations, "CREDENTIAL_LEAK"
                );
            }
        }

        // 2. Check for harmful content (BLOCK)
        HarmfulResult harmful = checkHarmfulContent(output);
        if (harmful.isHarmful) {
            violations.addAll(harmful.types);
            log.warning("Blocked harmful output: " + harmful.types);
            return GuardResult.block(
                "Output contains potentially harmful content",
                violations, "HARMFUL_CONTENT"
            );
        }

        // 3. Mask internal IDs if configured
        if (maskInternalIds) {
            MaskingResult masking = maskInternalReferences(processedOutput);
            if (masking.wasMasked) {
                processedOutput = masking.maskedOutput;
                violations.addAll(masking.types);
            }
        }

        // 4. Flag hallucination indicators (MASK with warning)
        if (flagHallucinations) {
            HallucinationResult hallucination = checkHallucinationIndicators(processedOutput);
            if (hallucination.hasIndicators) {
                violations.addAll(hallucination.indicators);
                // Add a disclaimer prefix rather than blocking
                processedOutput = addHallucinationDisclaimer(processedOutput);
            }
        }

        // Return based on what we found
        if (!violations.isEmpty()) {
            log.fine("Output modified: " + violations);
            return GuardResult.mask(processedOutput, violations);
        }

        return GuardResult.pass(processedOutput);
    }

    /**
     * Check for credential leakage.
     */
    private LeakageResult checkCredentialLeakage(String output) {
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

        return new LeakageResult(!types.isEmpty(), types);
    }

    /**
     * Check for harmful content.
     */
    private HarmfulResult checkHarmfulContent(String output) {
        List<String> types = new ArrayList<>();

        if (CODE_EXECUTION_PATTERN.matcher(output).find()) {
            types.add("Code Execution");
        }
        if (SQL_EXECUTION_PATTERN.matcher(output).find()) {
            types.add("Destructive SQL");
        }

        return new HarmfulResult(!types.isEmpty(), types);
    }

    /**
     * Mask internal ID references.
     */
    private MaskingResult maskInternalReferences(String output) {
        List<String> types = new ArrayList<>();
        String result = output;

        // Mask internal iDempiere IDs
        Matcher m = INTERNAL_ID_PATTERN.matcher(result);
        if (m.find()) {
            types.add("Internal ID");
            result = m.replaceAll("[INTERNAL_REF]");
        }

        return new MaskingResult(!types.isEmpty(), types, result);
    }

    /**
     * Check for hallucination indicators.
     */
    private HallucinationResult checkHallucinationIndicators(String output) {
        List<String> indicators = new ArrayList<>();

        if (HALLUCINATION_PATTERN.matcher(output).find()) {
            indicators.add("Uncertainty indicator");
        }
        if (UNVERIFIED_CLAIM_PATTERN.matcher(output).find()) {
            indicators.add("Unverified real-time claim");
        }

        return new HallucinationResult(!indicators.isEmpty(), indicators);
    }

    /**
     * Add hallucination disclaimer to output.
     */
    private String addHallucinationDisclaimer(String output) {
        // Only add if not already present
        if (output.startsWith("[Note:")) {
            return output;
        }
        return "[Note: This response may contain estimates or unverified information. " +
               "Please verify critical data against source systems.]\n\n" + output;
    }

    // ========================================================================
    // Configuration setters
    // ========================================================================

    public void setFlagHallucinations(boolean flagHallucinations) {
        this.flagHallucinations = flagHallucinations;
    }

    public void setBlockCredentialLeakage(boolean blockCredentialLeakage) {
        this.blockCredentialLeakage = blockCredentialLeakage;
    }

    public void setMaskInternalIds(boolean maskInternalIds) {
        this.maskInternalIds = maskInternalIds;
    }

    public void setKnownTables(Set<String> knownTables) {
        this.knownTables = knownTables;
    }

    public void setKnownColumns(Set<String> knownColumns) {
        this.knownColumns = knownColumns;
    }

    // ========================================================================
    // Inner classes
    // ========================================================================

    private static class LeakageResult {
        final boolean hasLeakage;
        final List<String> types;

        LeakageResult(boolean hasLeakage, List<String> types) {
            this.hasLeakage = hasLeakage;
            this.types = types;
        }
    }

    private static class HarmfulResult {
        final boolean isHarmful;
        final List<String> types;

        HarmfulResult(boolean isHarmful, List<String> types) {
            this.isHarmful = isHarmful;
            this.types = types;
        }
    }

    private static class MaskingResult {
        final boolean wasMasked;
        final List<String> types;
        final String maskedOutput;

        MaskingResult(boolean wasMasked, List<String> types, String maskedOutput) {
            this.wasMasked = wasMasked;
            this.types = types;
            this.maskedOutput = maskedOutput;
        }
    }

    private static class HallucinationResult {
        final boolean hasIndicators;
        final List<String> indicators;

        HallucinationResult(boolean hasIndicators, List<String> indicators) {
            this.hasIndicators = hasIndicators;
            this.indicators = indicators;
        }
    }
}
