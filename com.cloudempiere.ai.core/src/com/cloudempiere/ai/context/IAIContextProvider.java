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
package com.cloudempiere.ai.context;

import java.util.Properties;

import org.json.JSONObject;

/**
 * Generic interface for extracting context from iDempiere components
 * for AI processing.
 *
 * <p>Implementations exist for:
 * <ul>
 *   <li>Window/Tab context</li>
 *   <li>Dashboard/Chart context</li>
 *   <li>Process context</li>
 *   <li>Report context</li>
 *   <li>Form context</li>
 * </ul>
 *
 * <p>The context provider system enables AI features to understand
 * the user's current situation within iDempiere and provide relevant
 * assistance based on the component they're interacting with.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public interface IAIContextProvider {

    /**
     * Get the type of context this provider handles
     *
     * @return context type (e.g., "WINDOW", "CHART", "PROCESS", "DASHBOARD")
     */
    String getContextType();

    /**
     * Extract context information and serialize to JSON
     *
     * <p>This method captures all relevant information from the
     * iDempiere component needed for AI to understand the
     * user's current situation.
     *
     * <p>The returned JSON should include:
     * <ul>
     *   <li>User context (user, role, client, org)</li>
     *   <li>Component metadata (window, tab, chart, etc.)</li>
     *   <li>Current data/records</li>
     *   <li>Configuration (SQL, parameters, etc.)</li>
     *   <li>UI state (filters, selections, etc.)</li>
     * </ul>
     *
     * @param ctx iDempiere context (Properties)
     * @param windowNo window number (if applicable, use 0 for global)
     * @param parameters additional component-specific parameters
     * @return JSON object containing structured context
     * @throws IllegalArgumentException if required parameters are missing
     */
    JSONObject extractContext(
        Properties ctx,
        int windowNo,
        ContextParameters parameters
    );

    /**
     * Validate that extracted context is complete and valid
     *
     * <p>This method checks that all required fields are present
     * in the extracted context and that sensitive data has been
     * properly redacted.
     *
     * @param context extracted context JSON
     * @return true if context is valid and complete
     */
    boolean validateContext(JSONObject context);

    /**
     * Get security-sensitive fields that should be redacted
     *
     * <p>These fields will be automatically filtered out or masked
     * before sending context to AI providers to prevent exposure
     * of sensitive information.
     *
     * <p>Common sensitive fields include:
     * <ul>
     *   <li>Password, UserPIN</li>
     *   <li>CreditCardNumber, CreditCardVV</li>
     *   <li>TaxID, SSN</li>
     *   <li>BankAccountNo, IBAN</li>
     *   <li>APIKey, AccessToken</li>
     * </ul>
     *
     * @return array of field names to redact (case-insensitive)
     */
    String[] getSensitiveFields();

    /**
     * Get a human-readable description of this context provider
     *
     * @return description of the provider's purpose
     */
    default String getDescription() {
        return "Context provider for " + getContextType();
    }

    /**
     * Check if this provider supports streaming context extraction
     *
     * <p>Some providers may support streaming large datasets
     * incrementally rather than loading everything into memory.
     *
     * @return true if streaming is supported
     */
    default boolean supportsStreaming() {
        return false;
    }
}