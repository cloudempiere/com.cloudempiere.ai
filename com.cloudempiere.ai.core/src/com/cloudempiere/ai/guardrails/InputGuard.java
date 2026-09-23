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

import com.cloudempiere.ai.guardrails.detect.InjectionDetector;
import com.cloudempiere.ai.guardrails.detect.PiiDetector;
import com.cloudempiere.ai.guardrails.detect.SqlInjectionDetector;
import com.cloudempiere.ai.guardrails.dto.GuardResult;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;

/**
 * Input Guard for AI safety (ADR-014).
 *
 * <p>Orchestrates independent detectors - {@link InjectionDetector} (prompt injection,
 * system-prompt extraction), {@link SqlInjectionDetector}, and {@link PiiDetector} -
 * against user input, in that order:
 * <ul>
 *   <li>PASS - Input is safe, proceed</li>
 *   <li>MASK - PII detected and masked, proceed with masked input</li>
 *   <li>BLOCK - Dangerous input detected, reject request</li>
 * </ul>
 *
 * <p><b>Tags:</b> #guardrails #pii #input-validation #security #injection-prevention
 *
 * @author Cloudempiere AI Team
 * @version 2.0.0
 * @since v0.11.0
 * @see OutputGuard
 * @see com.cloudempiere.ai.guardrails.dto.GuardResult
 */
public class InputGuard implements InputGuardrail {

    private static final CLogger log = CLogger.getCLogger(InputGuard.class);

    /** Whether to mask PII or block entirely */
    private boolean maskPII = true;

    /** Whether to block on injection attempts */
    private boolean blockInjection = true;

    /** PII masking character */
    private String maskChar = "*";

    public InputGuard() {
    }

    public InputGuard(boolean maskPII, boolean blockInjection) {
        this.maskPII = maskPII;
        this.blockInjection = blockInjection;
    }

    /**
     * LangChain4j InputGuardrail entry point - wraps {@link #validate(String)} so this
     * guard can be attached directly via {@code @InputGuardrails(InputGuard.class)}.
     */
    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        GuardResult result = validate(userMessage.singleText());
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

        if (InjectionDetector.isPromptInjection(input)) {
            violations.add("Prompt injection detected");
            if (blockInjection) {
                log.warning("Blocked prompt injection attempt: " + summarize(input));
                return GuardResult.block("Input contains potentially harmful instructions",
                    violations, "INJECTION");
            }
        }

        if (InjectionDetector.isSystemPromptExtraction(input)) {
            violations.add("System prompt extraction attempt");
            log.warning("Blocked system prompt extraction: " + summarize(input));
            return GuardResult.block("Input appears to be attempting to extract system instructions",
                violations, "EXTRACTION");
        }

        if (SqlInjectionDetector.matches(input)) {
            violations.add("SQL injection pattern detected");
            log.warning("Blocked SQL injection pattern: " + summarize(input));
            return GuardResult.block("Input contains potentially harmful SQL patterns",
                violations, "SQL_INJECTION");
        }

        PiiDetector.Result piiResult = PiiDetector.detectAndMask(input, maskChar);
        if (piiResult.hasPII) {
            violations.addAll(piiResult.types);

            if (maskPII) {
                log.fine("Masked PII in input: " + piiResult.types);
                return GuardResult.mask(piiResult.maskedText, violations);
            } else {
                log.warning("Blocked input with PII: " + piiResult.types);
                return GuardResult.block("Input contains sensitive personal information",
                    violations, "PII");
            }
        }

        return GuardResult.pass(input);
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

    public void setMaskPII(boolean maskPII) {
        this.maskPII = maskPII;
    }

    public void setBlockInjection(boolean blockInjection) {
        this.blockInjection = blockInjection;
    }

    public void setMaskChar(String maskChar) {
        this.maskChar = maskChar;
    }
}
