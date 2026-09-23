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

import org.compiere.util.CLogger;

import com.cloudempiere.ai.guardrails.detect.CredentialLeakDetector;
import com.cloudempiere.ai.guardrails.detect.HallucinationDetector;
import com.cloudempiere.ai.guardrails.detect.HarmfulContentDetector;
import com.cloudempiere.ai.guardrails.detect.InternalIdMasker;
import com.cloudempiere.ai.guardrails.dto.GuardResult;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.guardrail.OutputGuardrail;
import dev.langchain4j.guardrail.OutputGuardrailResult;

/**
 * Output Guard for AI safety (ADR-014).
 *
 * <p>Orchestrates independent detectors - {@link CredentialLeakDetector},
 * {@link HarmfulContentDetector} (SQL check shared with {@code InputGuard} via
 * {@code SqlInjectionDetector}), {@link InternalIdMasker}, {@link HallucinationDetector} -
 * against LLM output, in that order:
 * <ul>
 *   <li>PASS - Output is safe, return to user</li>
 *   <li>MASK - Sensitive data detected and redacted</li>
 *   <li>BLOCK - Dangerous output detected, suppress response</li>
 * </ul>
 *
 * <p><b>Tags:</b> #guardrails #output-validation #data-leakage #hallucination #security
 *
 * @author Cloudempiere AI Team
 * @version 2.0.0
 * @since v0.11.0
 * @see InputGuard
 * @see com.cloudempiere.ai.guardrails.dto.GuardResult
 */
public class OutputGuard implements OutputGuardrail {

    private static final CLogger log = CLogger.getCLogger(OutputGuard.class);

    /** Whether to flag hallucination indicators */
    private boolean flagHallucinations = true;

    /** Whether to block credential leakage */
    private boolean blockCredentialLeakage = true;

    /** Whether to mask internal IDs */
    private boolean maskInternalIds = true;

    public OutputGuard() {
    }

    /**
     * LangChain4j OutputGuardrail entry point - wraps {@link #validate(String)} so this
     * guard can be attached directly via {@code @OutputGuardrails(OutputGuard.class)}.
     */
    @Override
    public OutputGuardrailResult validate(AiMessage aiMessage) {
        GuardResult result = validate(aiMessage.text());
        switch (result.getAction()) {
            case BLOCK:
                return failure(result.getBlockReason());
            case MASK:
                return successWith(result.getProcessedContent());
            default:
                return success();
        }
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

        if (blockCredentialLeakage) {
            List<String> leakTypes = CredentialLeakDetector.detect(output);
            if (!leakTypes.isEmpty()) {
                violations.addAll(leakTypes);
                log.warning("Blocked output with credential leakage: " + leakTypes);
                return GuardResult.block(
                    "Output contains sensitive credentials that cannot be displayed",
                    violations, "CREDENTIAL_LEAK"
                );
            }
        }

        List<String> harmfulTypes = HarmfulContentDetector.detect(output);
        if (!harmfulTypes.isEmpty()) {
            violations.addAll(harmfulTypes);
            log.warning("Blocked harmful output: " + harmfulTypes);
            return GuardResult.block(
                "Output contains potentially harmful content",
                violations, "HARMFUL_CONTENT"
            );
        }

        if (maskInternalIds) {
            InternalIdMasker.Result masking = InternalIdMasker.mask(processedOutput);
            if (masking.wasMasked) {
                processedOutput = masking.maskedOutput;
                violations.add("Internal ID");
            }
        }

        if (flagHallucinations) {
            List<String> indicators = HallucinationDetector.detect(processedOutput);
            if (!indicators.isEmpty()) {
                violations.addAll(indicators);
                processedOutput = addHallucinationDisclaimer(processedOutput);
            }
        }

        if (!violations.isEmpty()) {
            log.fine("Output modified: " + violations);
            return GuardResult.mask(processedOutput, violations);
        }

        return GuardResult.pass(processedOutput);
    }

    /**
     * Add hallucination disclaimer to output.
     */
    private String addHallucinationDisclaimer(String output) {
        if (output.startsWith("[Note:")) {
            return output;
        }
        return "[Note: This response may contain estimates or unverified information. " +
               "Please verify critical data against source systems.]\n\n" + output;
    }

    public void setFlagHallucinations(boolean flagHallucinations) {
        this.flagHallucinations = flagHallucinations;
    }

    public void setBlockCredentialLeakage(boolean blockCredentialLeakage) {
        this.blockCredentialLeakage = blockCredentialLeakage;
    }

    public void setMaskInternalIds(boolean maskInternalIds) {
        this.maskInternalIds = maskInternalIds;
    }
}
