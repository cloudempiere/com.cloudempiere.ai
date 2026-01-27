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
package com.cloudempiere.ai.kb.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of similarity analysis between new content and existing entries
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class SimilarityResult {
    private String userContent;
    private List<SimilarEntry> similarEntries;
    private boolean hasDuplicate; // Score > 0.85
    private boolean hasSimilar; // Score > 0.60
    private String analysisReasoning; // Why these are similar

    public SimilarityResult() {
        this.similarEntries = new ArrayList<>();
        this.hasDuplicate = false;
        this.hasSimilar = false;
    }

    /**
     * Represents a similar entry with score
     */
    public static class SimilarEntry implements Comparable<SimilarEntry> {
        private KnowledgeBaseEntry entry;
        private double similarityScore; // 0.0 to 1.0
        private String reason; // Why it's similar
        private boolean isDuplicate; // Score > 0.85

        public SimilarEntry(KnowledgeBaseEntry entry, double score) {
            this.entry = entry;
            this.similarityScore = score;
            this.isDuplicate = score > 0.85;
        }

        public KnowledgeBaseEntry getEntry() {
            return entry;
        }

        public void setEntry(KnowledgeBaseEntry entry) {
            this.entry = entry;
        }

        public double getSimilarityScore() {
            return similarityScore;
        }

        public void setSimilarityScore(double similarityScore) {
            this.similarityScore = similarityScore;
            this.isDuplicate = similarityScore > 0.85;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public boolean isDuplicate() {
            return isDuplicate;
        }

        @Override
        public int compareTo(SimilarEntry other) {
            // Sort by similarity score descending
            return Double.compare(other.similarityScore, this.similarityScore);
        }

        @Override
        public String toString() {
            return "SimilarEntry{" +
                    "entry=" + entry.getTitle() +
                    ", score=" + String.format("%.2f", similarityScore) +
                    ", isDuplicate=" + isDuplicate +
                    '}';
        }
    }

    // Getters and Setters
    public String getUserContent() {
        return userContent;
    }

    public void setUserContent(String userContent) {
        this.userContent = userContent;
    }

    public List<SimilarEntry> getSimilarEntries() {
        return similarEntries;
    }

    public void addSimilarEntry(SimilarEntry entry) {
        this.similarEntries.add(entry);
        if (entry.isDuplicate()) {
            this.hasDuplicate = true;
        }
        if (entry.getSimilarityScore() > 0.60) {
            this.hasSimilar = true;
        }
    }

    public boolean isHasDuplicate() {
        return hasDuplicate;
    }

    public boolean isHasSimilar() {
        return hasSimilar;
    }

    public String getAnalysisReasoning() {
        return analysisReasoning;
    }

    public void setAnalysisReasoning(String analysisReasoning) {
        this.analysisReasoning = analysisReasoning;
    }

    /**
     * Get top N similar entries
     */
    public List<SimilarEntry> getTopSimilar(int count) {
        List<SimilarEntry> sorted = new ArrayList<>(similarEntries);
        sorted.sort(SimilarEntry::compareTo);
        return sorted.subList(0, Math.min(count, sorted.size()));
    }

    @Override
    public String toString() {
        return "SimilarityResult{" +
                "hasDuplicate=" + hasDuplicate +
                ", hasSimilar=" + hasSimilar +
                ", similarCount=" + similarEntries.size() +
                '}';
    }
}
