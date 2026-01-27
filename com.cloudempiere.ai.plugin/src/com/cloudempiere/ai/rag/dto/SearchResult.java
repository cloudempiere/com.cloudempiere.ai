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
package com.cloudempiere.ai.rag.dto;

import java.util.Map;

/**
 * Result from RAG semantic search.
 *
 * <p>Contains the matched content, source information, and relevance score.
 *
 * @author Cloudempiere AI Team
 * @version ADR-012
 * @since v0.11.0
 */
public class SearchResult {

    private final String id;
    private final String title;
    private final String content;
    private final String sourceType;
    private final String sourceId;
    private final double score;
    private final Map<String, Object> metadata;

    /**
     * Create a search result.
     *
     * @param id Unique identifier (UUID)
     * @param title Display title
     * @param content Text content
     * @param sourceType Source type identifier
     * @param sourceId Source record ID
     * @param score Similarity score (0.0 - 1.0)
     * @param metadata Additional metadata
     */
    public SearchResult(String id, String title, String content, String sourceType,
                       String sourceId, double score, Map<String, Object> metadata) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.score = score;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public double getScore() {
        return score;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Get a formatted string for display or agent consumption.
     *
     * @return Formatted result string
     */
    public String toFormattedString() {
        StringBuilder sb = new StringBuilder();
        sb.append("**").append(title != null ? title : "Untitled").append("**\n");
        sb.append(content).append("\n");
        sb.append("_Source: ").append(sourceType);
        if (sourceId != null && !sourceId.isEmpty()) {
            sb.append(" (").append(sourceId).append(")");
        }
        sb.append(" | Score: ").append(String.format("%.2f", score)).append("_");
        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("SearchResult[id=%s, title=%s, source=%s, score=%.2f]",
                id, title, sourceType, score);
    }

    /**
     * Builder for SearchResult.
     */
    public static class Builder {
        private String id;
        private String title;
        private String content;
        private String sourceType;
        private String sourceId;
        private double score;
        private Map<String, Object> metadata;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder sourceType(String sourceType) {
            this.sourceType = sourceType;
            return this;
        }

        public Builder sourceId(String sourceId) {
            this.sourceId = sourceId;
            return this;
        }

        public Builder score(double score) {
            this.score = score;
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public SearchResult build() {
            return new SearchResult(id, title, content, sourceType, sourceId, score, metadata);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
