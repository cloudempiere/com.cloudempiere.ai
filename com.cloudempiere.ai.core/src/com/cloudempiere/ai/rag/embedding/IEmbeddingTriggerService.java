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
package com.cloudempiere.ai.rag.embedding;

import java.util.Properties;

/**
 * Interface for trigger-based embedding operations.
 *
 * <p>This service is called by event handlers when embeddable entities
 * (K_Entry, AD_Window, etc.) are created, updated, or deleted.
 *
 * <p>Features:
 * <ul>
 *   <li>Synchronous embedding on entity change</li>
 *   <li>Content hash-based deduplication</li>
 *   <li>Multi-tenant aware</li>
 * </ul>
 *
 * @author Cloudempiere AI Team
 * @version ADR-029
 * @since v0.12.0
 */
public interface IEmbeddingTriggerService {

    /**
     * Add or update an embedding for an entity.
     *
     * <p>If an embedding already exists for this source, it will be replaced.
     * If the content hash matches the existing embedding, the operation is skipped.
     *
     * @param ctx iDempiere context (for AD_Client_ID, AD_Org_ID)
     * @param sourceType Type identifier (e.g., "knowledge_entry", "ad_metadata")
     * @param sourceId Entity ID (e.g., K_Entry_ID as string)
     * @param sourceTable Table name (e.g., "K_Entry")
     * @param textContent Text content to embed
     * @param title Optional title for metadata
     * @param language Optional AD_Language
     * @return Result with status and embedding ID
     */
    EmbeddingResult addOrUpdateEmbedding(Properties ctx, String sourceType, String sourceId,
            String sourceTable, String textContent, String title, String language);

    /**
     * Delete embedding(s) for an entity.
     *
     * @param ctx iDempiere context
     * @param sourceType Type identifier
     * @param sourceId Entity ID
     * @return Number of embeddings deleted
     */
    int deleteEmbedding(Properties ctx, String sourceType, String sourceId);

    /**
     * Check if an embedding exists for an entity.
     *
     * @param ctx iDempiere context
     * @param sourceType Type identifier
     * @param sourceId Entity ID
     * @return true if embedding exists
     */
    boolean hasEmbedding(Properties ctx, String sourceType, String sourceId);

    /**
     * Check if the service is available.
     *
     * @return true if embedding operations are possible
     */
    boolean isAvailable();

    /**
     * Result of an embedding operation.
     */
    public static class EmbeddingResult {
        private final boolean success;
        private final boolean skipped;
        private final String embeddingId;
        private final String status;
        private final String errorMessage;
        private final long durationMs;

        private EmbeddingResult(boolean success, boolean skipped, String embeddingId,
                String status, String errorMessage, long durationMs) {
            this.success = success;
            this.skipped = skipped;
            this.embeddingId = embeddingId;
            this.status = status;
            this.errorMessage = errorMessage;
            this.durationMs = durationMs;
        }

        public static EmbeddingResult success(String embeddingId, String status, long durationMs) {
            return new EmbeddingResult(true, false, embeddingId, status, null, durationMs);
        }

        public static EmbeddingResult skipped(String reason) {
            return new EmbeddingResult(true, true, null, "skipped", reason, 0);
        }

        public static EmbeddingResult failed(String errorMessage) {
            return new EmbeddingResult(false, false, null, "failed", errorMessage, 0);
        }

        public boolean isSuccess() {
            return success;
        }

        public boolean isSkipped() {
            return skipped;
        }

        public String getEmbeddingId() {
            return embeddingId;
        }

        public String getStatus() {
            return status;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public long getDurationMs() {
            return durationMs;
        }

        @Override
        public String toString() {
            if (skipped) {
                return "EmbeddingResult[skipped: " + errorMessage + "]";
            }
            if (!success) {
                return "EmbeddingResult[FAILED: " + errorMessage + "]";
            }
            return "EmbeddingResult[" + status + ", id=" + embeddingId + ", " + durationMs + "ms]";
        }
    }
}
