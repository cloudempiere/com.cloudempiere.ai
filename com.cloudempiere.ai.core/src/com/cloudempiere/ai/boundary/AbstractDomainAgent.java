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
package com.cloudempiere.ai.boundary;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Shared {@link IDomainAgent} logic for the 5 domain plugins (sales, inventory,
 * purchasing, support, kb).
 *
 * <p>Deliberately does NOT extract the OSGi {@code @Reference}/{@code @Activate}/
 * {@code ensureInitialized()} lifecycle - that stays in each {@code @Component}-annotated
 * subclass, since DS field injection into an abstract base needs to be verified against
 * a live OSGi runtime this review couldn't do. What's safe to share without runtime risk:
 * keyword-based routing (with proper word-boundary matching - a bare {@code contains("po")}
 * false-matches "opportunity", "support", "report"), and folding the window/record
 * {@code context} map into the query text before it reaches the agent, since every
 * subclass's {@code process()} was previously discarding it entirely despite
 * {@link IDomainAgent#process}'s own contract saying context should be used.
 *
 * @author Cloudempiere AI Team
 */
public abstract class AbstractDomainAgent implements IDomainAgent {

    private final String domainName;
    private final String[] keywords;
    private final String[] tableNameHints;

    protected AbstractDomainAgent(String domainName, String[] keywords, String[] tableNameHints) {
        this.domainName = domainName;
        this.keywords = keywords;
        this.tableNameHints = tableNameHints;
    }

    @Override
    public String getDomain() {
        return domainName;
    }

    @Override
    public boolean canHandle(String query, Map<String, Object> context) {
        if (query != null) {
            String lower = query.toLowerCase();
            for (String keyword : keywords) {
                if (containsKeyword(lower, keyword)) {
                    return true;
                }
            }
        }
        if (context != null && context.get("tableName") != null) {
            String tableName = context.get("tableName").toString().toLowerCase();
            for (String hint : tableNameHints) {
                if (tableName.contains(hint)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String process(String query, Map<String, Object> context) {
        return chat(formatQueryWithContext(query, context));
    }

    /**
     * Send a query (already including any window/record context) to this domain's agent.
     */
    protected abstract String chat(String query);

    /**
     * Fold the window/record context map (see {@code WindowContextExtractor}) into the
     * query text, the same "[Context]...\n\nUser question: ..." shape {@code AIService}
     * already uses for its own context injection.
     */
    protected String formatQueryWithContext(String query, Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return query;
        }

        StringBuilder sb = new StringBuilder("[Context: the user is viewing a record");
        Object tableName = context.get("tableName");
        if (tableName != null) {
            sb.append(" in ").append(tableName);
        }
        Object recordId = context.get("recordId");
        if (recordId != null) {
            sb.append(", record ID ").append(recordId);
        }
        Object tabName = context.get("tabName");
        if (tabName != null) {
            sb.append(" (tab: ").append(tabName).append(")");
        }
        Object fields = context.get("fields");
        if (fields instanceof Map<?, ?> fieldMap && !fieldMap.isEmpty()) {
            sb.append(". Visible field values: ");
            boolean first = true;
            for (Map.Entry<?, ?> entry : fieldMap.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(entry.getKey()).append('=').append(entry.getValue());
                first = false;
            }
        }
        sb.append("]\n\nUser question: ").append(query);
        return sb.toString();
    }

    /**
     * Word-boundary keyword match. A bare {@code String.contains(keyword)} false-matches
     * short tokens as substrings of unrelated words (e.g. "po" inside "opportunity",
     * "support", "report") - use {@code \b} boundaries for single-word keywords. Multi-word
     * phrases (e.g. "purchase order") are safe with a plain substring check since the space
     * already prevents accidental mid-word matches.
     */
    protected static boolean containsKeyword(String lowerCaseHaystack, String keyword) {
        if (keyword.indexOf(' ') >= 0) {
            return lowerCaseHaystack.contains(keyword);
        }
        return Pattern.compile("\\b" + Pattern.quote(keyword) + "\\b").matcher(lowerCaseHaystack).find();
    }
}
