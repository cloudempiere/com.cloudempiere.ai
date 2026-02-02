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
package com.cloudempiere.ai.rag.ingest;

import java.sql.Timestamp;
import java.util.Properties;

/**
 * Interface for knowledge ingestion into embedding store.
 *
 * <p>Implementations extract knowledge from various sources (AD metadata,
 * K_Entry articles, glossary, etc.) and create embeddings for semantic search.
 *
 * <p>Reference: idempiere-cli ingestors
 *
 * @author Cloudempiere AI Team
 * @version ADR-012
 * @since v0.11.0
 */
public interface IKnowledgeIngestor {

    /**
     * Get unique identifier for this ingestor.
     *
     * <p>Used for:
     * <ul>
     *   <li>Metadata filtering (source_type column)</li>
     *   <li>Ingestion tracking (aig_ingestion_metadata)</li>
     *   <li>Selective re-ingestion</li>
     * </ul>
     *
     * @return Source type identifier (e.g., "ad_metadata", "knowledge_entry")
     */
    String getSourceType();

    /**
     * Get human-readable name for this ingestor.
     *
     * @return Display name
     */
    String getDisplayName();

    /**
     * Ingest knowledge from source into embedding store.
     *
     * <p>Process:
     * <ol>
     *   <li>Query source data</li>
     *   <li>Transform to text segments</li>
     *   <li>Generate embeddings</li>
     *   <li>Store with metadata</li>
     *   <li>Update ingestion tracking</li>
     * </ol>
     *
     * @param ctx iDempiere context (for AD_Client_ID, AD_Org_ID)
     * @param forceRefresh If true, clear existing and re-ingest all
     * @return Result with counts and timing
     */
    IngestResult ingest(Properties ctx, boolean forceRefresh);

    /**
     * Check if ingestion is needed.
     *
     * <p>Compares last ingestion timestamp with source data timestamps.
     *
     * @param ctx iDempiere context
     * @return true if source has changed since last ingestion
     */
    boolean needsRefresh(Properties ctx);

    /**
     * Get timestamp of last successful ingestion.
     *
     * @param ctx iDempiere context
     * @return Last ingestion timestamp, or null if never ingested
     */
    Timestamp getLastIngestion(Properties ctx);

    /**
     * Get estimated document count for this source.
     *
     * <p>Used for progress reporting and capacity planning.
     *
     * @param ctx iDempiere context
     * @return Estimated number of documents to ingest
     */
    int getEstimatedCount(Properties ctx);

    /**
     * Get priority for ingestion order.
     *
     * <p>Lower numbers are ingested first.
     *
     * @return Priority (default 100)
     */
    default int getPriority() {
        return 100;
    }
}
