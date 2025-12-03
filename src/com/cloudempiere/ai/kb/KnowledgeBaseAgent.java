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
package com.cloudempiere.ai.kb;

import org.compiere.util.CLogger;
import org.json.JSONObject;

import com.cloudempiere.ai.context.AIContextProviderRegistry;
import com.cloudempiere.ai.context.ContextParameters;
import com.cloudempiere.ai.context.IAIContextProvider;
import com.cloudempiere.ai.kb.analysis.KnowledgeBaseSimilarityAnalyzer;
import com.cloudempiere.ai.kb.dto.KnowledgeBaseEntry;
import com.cloudempiere.ai.kb.dto.KnowledgeBaseHierarchy;
import com.cloudempiere.ai.kb.dto.PlacementRecommendation;
import com.cloudempiere.ai.kb.dto.SimilarityResult;
import com.cloudempiere.ai.provider.IAIProvider;
import com.cloudempiere.ai.provider.dto.AIMessage;
import com.cloudempiere.ai.provider.dto.AIRequest;
import com.cloudempiere.ai.provider.dto.AIResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Knowledge Base Agent - Specialized processor for knowledge base content
 *
 * <p>Orchestrates the workflow of:
 * 1. Reading KB navigation tree via context provider
 * 2. Parsing existing content (editor.js → markdown)
 * 3. Finding similar/duplicate entries
 * 4. Using AI to analyze and recommend placement
 * 5. Returning structured recommendations
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class KnowledgeBaseAgent {

    private static final CLogger log = CLogger.getCLogger(KnowledgeBaseAgent.class);

    private final IAIProvider aiProvider;

    public KnowledgeBaseAgent(IAIProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    /**
     * Analyze user content and recommend placement in knowledge base
     *
     * @param ctx iDempiere context
     * @param kType knowledge base type
     * @param userContent user-provided content to be added
     * @param userTitle suggested title (optional)
     * @return placement recommendation
     */
    public PlacementRecommendation analyzePlacement(
        Properties ctx,
        String kType,
        String userContent,
        String userTitle
    ) throws Exception {
        log.fine("Analyzing KB placement for content: " + userTitle);

        try {
            // 1. Load knowledge base hierarchy
            KnowledgeBaseHierarchy hierarchy = loadKnowledgeBaseHierarchy(ctx, kType);
            if (hierarchy.getTotalEntries() == 0) {
                PlacementRecommendation rec = new PlacementRecommendation();
                rec.setAction(PlacementRecommendation.Action.CREATE_NEW);
                rec.setTitle(userTitle);
                rec.setConfidence(1.0);
                rec.setReasoning("Knowledge base is empty, creating root entry");
                return rec;
            }

            // 2. Find similar entries
            SimilarityResult similarity = KnowledgeBaseSimilarityAnalyzer
                .analyzeSimilarity(userContent, hierarchy);

            // 3. Ask AI to analyze and recommend
            PlacementRecommendation recommendation = askAIForRecommendation(
                ctx,
                hierarchy,
                userContent,
                userTitle,
                similarity
            );

            log.fine("Recommendation: " + recommendation);
            return recommendation;

        } catch (Exception e) {
            log.log(Level.SEVERE, "Error analyzing KB placement", e);
            throw e;
        }
    }

    /**
     * Load knowledge base hierarchy
     */
    private KnowledgeBaseHierarchy loadKnowledgeBaseHierarchy(Properties ctx, String kType)
        throws Exception {
        IAIContextProvider provider = AIContextProviderRegistry
            .getInstance()
            .getProvider("KNOWLEDGE_BASE");

        if (provider == null) {
            throw new IllegalStateException("KnowledgeBaseContextProvider not registered");
        }

        ContextParameters params = new ContextParameters();
        params.put("k_type", kType);

        JSONObject contextJson = provider.extractContext(ctx, 0, params);
        if (!contextJson.optBoolean("success", false)) {
            throw new Exception("Failed to load KB hierarchy: " +
                                contextJson.optString("error", "Unknown error"));
        }

        // For now, we're returning a structured result from the provider
        // In real implementation, we'd parse and rebuild the hierarchy object
        KnowledgeBaseHierarchy hierarchy = new KnowledgeBaseHierarchy(kType);
        // Note: Hierarchy loading would happen in the context provider
        // This is a simplified version
        return hierarchy;
    }

    /**
     * Ask AI to analyze and recommend placement
     */
    private PlacementRecommendation askAIForRecommendation(
        Properties ctx,
        KnowledgeBaseHierarchy hierarchy,
        String userContent,
        String userTitle,
        SimilarityResult similarity
    ) throws Exception {
        // Build analysis prompt for AI
        String analysisPrompt = buildAnalysisPrompt(
            hierarchy,
            userContent,
            userTitle,
            similarity
        );

        // Create AIRequest
        AIRequest request = AIRequest.builder()
            .withModel("claude-3-5-sonnet-20241022")
            .withTemperature(0.3) // Lower for more deterministic output
            .addMessage(new AIMessage("user", analysisPrompt))
            .build();

        // Call AI provider
        AIResponse response = aiProvider.generateText(request);

        // Parse AI response into recommendation
        PlacementRecommendation recommendation = parseAIResponse(
            response.getContent(),
            hierarchy,
            similarity
        );

        return recommendation;
    }

    /**
     * Build analysis prompt for AI
     */
    private String buildAnalysisPrompt(
        KnowledgeBaseHierarchy hierarchy,
        String userContent,
        String userTitle,
        SimilarityResult similarity
    ) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a Knowledge Base Assistant. Analyze the new content ")
              .append("and recommend where it should be placed in the knowledge base.\n\n");

        prompt.append("# Knowledge Base Structure\n");
        prompt.append("Knowledge Base Type: ").append(hierarchy.getK_type()).append("\n");
        prompt.append("Total Entries: ").append(hierarchy.getTotalEntries()).append("\n");
        prompt.append("Navigation Tree:\n").append(hierarchy.getTreeStructure()).append("\n\n");

        prompt.append("# New Content to Add\n");
        if (userTitle != null && !userTitle.isEmpty()) {
            prompt.append("Title: ").append(userTitle).append("\n");
        }
        prompt.append("Content:\n").append(userContent).append("\n\n");

        // Add similar entries info
        if (similarity.isHasDuplicate()) {
            prompt.append("# DUPLICATE WARNING\n");
            prompt.append("This content appears to be a duplicate of:\n");
            for (SimilarityResult.SimilarEntry similar : similarity.getTopSimilar(3)) {
                prompt.append("- ").append(similar.getEntry().getTitle())
                      .append(" (Score: ").append(String.format("%.2f", similar.getSimilarityScore()))
                      .append(")\n");
            }
            prompt.append("\n");
        } else if (similarity.isHasSimilar()) {
            prompt.append("# Similar Entries Found\n");
            for (SimilarityResult.SimilarEntry similar : similarity.getTopSimilar(5)) {
                prompt.append("- ").append(similar.getEntry().getTitle())
                      .append(" (Score: ").append(String.format("%.2f", similar.getSimilarityScore()))
                      .append("): ").append(similar.getReason()).append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("# Analysis Instructions\n");
        prompt.append("Based on the structure and content, provide a recommendation:\n");
        prompt.append("1. Should this be a NEW ENTRY or added to EXISTING entry?\n");
        prompt.append("2. If new: What should be its PARENT (or is it a root entry)?\n");
        prompt.append("3. What is your CONFIDENCE (0-100)?\n");
        prompt.append("4. What best practices from the KB structure were applied?\n");
        prompt.append("5. Any WARNINGS about duplicates or conflicts?\n\n");

        prompt.append("Respond in this JSON format:\n");
        prompt.append("{\n");
        prompt.append("  \"action\": \"CREATE_NEW\" or \"EXTEND_EXISTING\" or \"UPDATE_EXISTING\",\n");
        prompt.append("  \"target_entry_id\": [if extending/updating existing],\n");
        prompt.append("  \"suggested_parent_id\": [if creating new, parent entry id or 0 for root],\n");
        prompt.append("  \"confidence\": [0-100],\n");
        prompt.append("  \"title\": \"[suggested title if creating new]\",\n");
        prompt.append("  \"reasoning\": \"[why this placement makes sense]\",\n");
        prompt.append("  \"best_practices\": [\"practice 1\", \"practice 2\"],\n");
        prompt.append("  \"warning\": \"[if any duplicate risk or conflict]\"\n");
        prompt.append("}\n");

        return prompt.toString();
    }

    /**
     * Parse AI response into PlacementRecommendation
     */
    private PlacementRecommendation parseAIResponse(
        String aiResponse,
        KnowledgeBaseHierarchy hierarchy,
        SimilarityResult similarity
    ) {
        PlacementRecommendation rec = new PlacementRecommendation();

        try {
            // Extract JSON from response (AI might wrap it in markdown code blocks)
            String jsonStr = aiResponse;
            if (jsonStr.contains("```json")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7);
                jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
            } else if (jsonStr.contains("```")) {
                jsonStr = jsonStr.substring(jsonStr.indexOf("```") + 3);
                jsonStr = jsonStr.substring(0, jsonStr.indexOf("```"));
            }

            JSONObject responseJson = new JSONObject(jsonStr);

            // Parse action
            String actionStr = responseJson.optString("action", "CREATE_NEW");
            rec.setAction(PlacementRecommendation.Action.valueOf(actionStr));

            // Parse IDs
            if (responseJson.has("target_entry_id")) {
                rec.setTargetEntryId(responseJson.getInt("target_entry_id"));
            }
            if (responseJson.has("suggested_parent_id")) {
                rec.setSuggestedParentId(responseJson.getInt("suggested_parent_id"));
            }

            // Parse confidence
            int confidence = responseJson.optInt("confidence", 50);
            rec.setConfidence(confidence / 100.0);

            // Other fields
            rec.setTitle(responseJson.optString("title", null));
            rec.setReasoning(responseJson.optString("reasoning", ""));

            // Best practices
            if (responseJson.has("best_practices")) {
                for (Object practice : responseJson.getJSONArray("best_practices")) {
                    rec.addBestPractice(practice.toString());
                }
            }

            // Warning
            if (responseJson.has("warning")) {
                rec.setWarningMessage(responseJson.getString("warning"));
            }

            // Add duplicate warning from similarity analysis
            if (similarity.isHasDuplicate()) {
                rec.setAction(PlacementRecommendation.Action.DUPLICATE_WARNING);
                StringBuilder warning = new StringBuilder("Potential duplicate detected: ");
                for (SimilarityResult.SimilarEntry similar : similarity.getTopSimilar(1)) {
                    warning.append(similar.getEntry().getTitle());
                }
                rec.setWarningMessage(warning.toString());
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "Error parsing AI response, using default recommendation", e);
            rec.setAction(PlacementRecommendation.Action.CREATE_NEW);
            rec.setConfidence(0.5);
            rec.setReasoning("Manual review recommended - AI analysis failed");
        }

        return rec;
    }
}
