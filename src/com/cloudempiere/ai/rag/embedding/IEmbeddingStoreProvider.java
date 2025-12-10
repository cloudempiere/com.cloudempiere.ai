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

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * OSGi service interface for embedding store access.
 *
 * <p>Provides persistent vector storage for RAG-based context retrieval.
 * Replaces the InMemoryEmbeddingStore with pgvector-backed persistence.
 *
 * <p>This interface follows the OSGi service pattern, allowing different
 * implementations (pgvector, in-memory for testing, etc.) to be swapped
 * via declarative services.
 *
 * <p>Reference: idempiere-cli EmbeddingStoreProvider (Quarkus CDI pattern)
 *
 * @author Cloudempiere AI Team
 * @version ADR-026
 * @since v0.11.0
 * @see EmbeddingStoreProvider
 */
public interface IEmbeddingStoreProvider {

    /**
     * Get the embedding store for vector operations.
     *
     * <p>Returns a thread-safe embedding store that can be used for:
     * <ul>
     *   <li>Adding embeddings with segments</li>
     *   <li>Searching for similar vectors</li>
     *   <li>Filtering by metadata</li>
     * </ul>
     *
     * @return EmbeddingStore instance (never null, may be fallback in-memory)
     */
    EmbeddingStore<TextSegment> getStore();

    /**
     * Check if persistent storage (pgvector) is available.
     *
     * <p>Returns false if:
     * <ul>
     *   <li>pgvector extension not installed</li>
     *   <li>Database connection failed</li>
     *   <li>aig_embedding table not found</li>
     * </ul>
     *
     * @return true if pgvector is available and configured
     */
    boolean isAvailable();

    /**
     * Check if operating in fallback mode (in-memory).
     *
     * <p>Fallback mode is used when pgvector is unavailable but the
     * application should continue with reduced functionality.
     *
     * @return true if using in-memory fallback instead of pgvector
     */
    boolean isFallbackMode();

    /**
     * Get the configured embedding dimension.
     *
     * <p>Common dimensions:
     * <ul>
     *   <li>768 - nomic-embed-text, all-MiniLM-L6-v2</li>
     *   <li>1024 - Amazon Titan Embed v2</li>
     *   <li>1536 - OpenAI text-embedding-3-small</li>
     *   <li>3072 - OpenAI text-embedding-3-large</li>
     * </ul>
     *
     * @return Embedding dimension configured for the store
     */
    int getDimension();

    /**
     * Get the table name used for storage.
     *
     * @return Table name (default: "aig_embedding")
     */
    String getTableName();

    /**
     * Clear all embeddings for a specific source type.
     *
     * <p>Used for re-ingestion of knowledge sources.
     *
     * @param sourceType Source type to clear (e.g., "ad_metadata")
     * @param clientId AD_Client_ID for multi-tenant filtering
     * @return Number of records deleted
     */
    int clearBySourceType(String sourceType, int clientId);

    /**
     * Get statistics about the embedding store.
     *
     * @param clientId AD_Client_ID for filtering
     * @return Statistics map with counts by source type
     */
    java.util.Map<String, Object> getStatistics(int clientId);
}
