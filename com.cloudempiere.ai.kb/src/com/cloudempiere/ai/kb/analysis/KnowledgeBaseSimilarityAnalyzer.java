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
package com.cloudempiere.ai.kb.analysis;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.compiere.util.CLogger;

import com.cloudempiere.ai.kb.dto.KnowledgeBaseEntry;
import com.cloudempiere.ai.kb.dto.KnowledgeBaseHierarchy;
import com.cloudempiere.ai.kb.dto.SimilarityResult;
import com.cloudempiere.ai.kb.dto.SimilarityResult.SimilarEntry;
import com.cloudempiere.ai.util.EditorJsParser;

/**
 * Analyzer for finding similar knowledge base entries
 *
 * <p>Uses keyword matching and content analysis to find similar/duplicate
 * entries in the knowledge base. Uses simple TF-IDF-like scoring.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class KnowledgeBaseSimilarityAnalyzer {

    private static final CLogger log = CLogger.getCLogger(KnowledgeBaseSimilarityAnalyzer.class);

    /**
     * Analyze user content for similar entries in knowledge base
     */
    public static SimilarityResult analyzeSimilarity(
        String userContent,
        KnowledgeBaseHierarchy hierarchy
    ) {
        SimilarityResult result = new SimilarityResult();
        result.setUserContent(userContent);

        if (userContent == null || userContent.trim().isEmpty()) {
            return result;
        }

        try {
            // Extract keywords from user content
            Set<String> userKeywords = extractKeywords(userContent);

            if (userKeywords.isEmpty()) {
                return result;
            }

            // Compare against all entries
            for (KnowledgeBaseEntry entry : hierarchy.getAllEntries()) {
                double score = calculateSimilarity(userContent, userKeywords, entry);

                if (score > 0.30) { // Include moderate matches
                    SimilarEntry similar = new SimilarEntry(entry, score);
                    similar.setReason(generateReason(userKeywords, entry));
                    result.addSimilarEntry(similar);
                }
            }

            // Sort by score
            result.getSimilarEntries().sort(SimilarEntry::compareTo);

        } catch (Exception e) {
            log.log(Level.WARNING, "Error during similarity analysis", e);
        }

        return result;
    }

    /**
     * Calculate similarity score between user content and knowledge base entry
     */
    private static double calculateSimilarity(
        String userContent,
        Set<String> userKeywords,
        KnowledgeBaseEntry entry
    ) {
        // Title match (higher weight)
        double titleScore = calculateTextSimilarity(userContent, entry.getTitle()) * 0.4;

        // Description match
        double descScore = 0;
        if (entry.getDescription() != null && !entry.getDescription().isEmpty()) {
            descScore = calculateTextSimilarity(userContent, entry.getDescription()) * 0.25;
        }

        // Content match (markdown)
        double contentScore = 0;
        if (entry.getContentMarkdown() != null && !entry.getContentMarkdown().isEmpty()) {
            contentScore = calculateTextSimilarity(userContent, entry.getContentMarkdown()) * 0.35;
        }

        return titleScore + descScore + contentScore;
    }

    /**
     * Calculate similarity between two texts using keyword overlap
     */
    private static double calculateTextSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isEmpty() || text2.isEmpty()) {
            return 0.0;
        }

        Set<String> keywords1 = extractKeywords(text1);
        Set<String> keywords2 = extractKeywords(text2);

        if (keywords1.isEmpty() || keywords2.isEmpty()) {
            return 0.0;
        }

        // Jaccard similarity: intersection / union
        Set<String> intersection = new HashSet<>(keywords1);
        intersection.retainAll(keywords2);

        Set<String> union = new HashSet<>(keywords1);
        union.addAll(keywords2);

        return (double) intersection.size() / union.size();
    }

    /**
     * Extract meaningful keywords from text
     */
    private static Set<String> extractKeywords(String text) {
        Set<String> keywords = new HashSet<>();

        if (text == null || text.isEmpty()) {
            return keywords;
        }

        // Convert to plain text if markdown
        String plainText = EditorJsParser.markdownToPlainText(text);

        // Split into words
        String[] words = plainText.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "") // Remove special chars
            .split("\\s+");

        // Filter: words with 3+ chars, exclude common words
        Set<String> stopWords = getStopWords();
        for (String word : words) {
            if (word.length() >= 3 && !stopWords.contains(word)) {
                keywords.add(word);
            }
        }

        return keywords;
    }

    /**
     * Get common stop words to exclude
     */
    private static Set<String> getStopWords() {
        Set<String> stopWords = new HashSet<>();
        String[] words = {
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "is", "are", "was", "were", "be", "have", "has", "do", "does",
            "did", "will", "would", "could", "should", "may", "might", "can",
            "this", "that", "these", "those", "i", "you", "he", "she", "it",
            "we", "they", "what", "which", "who", "when", "where", "why", "how"
        };
        for (String word : words) {
            stopWords.add(word);
        }
        return stopWords;
    }

    /**
     * Generate reason for why entries are similar
     */
    private static String generateReason(Set<String> userKeywords, KnowledgeBaseEntry entry) {
        Set<String> entryKeywords = new HashSet<>();

        if (entry.getTitle() != null) {
            entryKeywords.addAll(extractKeywords(entry.getTitle()));
        }
        if (entry.getDescription() != null) {
            entryKeywords.addAll(extractKeywords(entry.getDescription()));
        }

        Set<String> commonKeywords = new HashSet<>(userKeywords);
        commonKeywords.retainAll(entryKeywords);

        if (commonKeywords.isEmpty()) {
            return "Content similarity detected";
        }

        StringBuilder reason = new StringBuilder("Shared keywords: ");
        int count = 0;
        for (String keyword : commonKeywords) {
            if (count > 0) reason.append(", ");
            reason.append(keyword);
            if (++count >= 3) break; // Show top 3
        }

        return reason.toString();
    }
}
