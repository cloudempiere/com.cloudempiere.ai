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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.compiere.db.CConnection;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Ini;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;

/**
 * OSGi service providing pgvector-backed embedding store.
 *
 * <p>This service provides persistent vector storage for RAG context retrieval,
 * replacing volatile InMemoryEmbeddingStore with PostgreSQL pgvector.
 *
 * <p>Features:
 * <ul>
 *   <li>Automatic fallback to in-memory if pgvector unavailable</li>
 *   <li>Connection parameters from iDempiere's CConnection</li>
 *   <li>Multi-tenant aware (filtering by AD_Client_ID)</li>
 *   <li>HNSW index for fast similarity search</li>
 * </ul>
 *
 * <p>Configuration:
 * <ul>
 *   <li>Table: aig_embedding (created by migration)</li>
 *   <li>Dimension: 768 (nomic-embed-text default)</li>
 *   <li>Index: HNSW with cosine distance</li>
 * </ul>
 *
 * @author Cloudempiere AI Team
 * @version ADR-026
 * @since v0.11.0
 */
@Component(
    service = IEmbeddingStoreProvider.class,
    immediate = true,
    property = {
        "service.ranking:Integer=100"
    }
)
public class EmbeddingStoreProvider implements IEmbeddingStoreProvider {

    private static final CLogger log = CLogger.getCLogger(EmbeddingStoreProvider.class);

    /** Default embedding dimension (nomic-embed-text) */
    private static final int DEFAULT_DIMENSION = 768;

    /** Table name for embeddings */
    private static final String TABLE_NAME = "aig_embedding";

    /** pgvector embedding store (null if unavailable) */
    private PgVectorEmbeddingStore pgVectorStore;

    /** Fallback in-memory store */
    private InMemoryEmbeddingStore<TextSegment> inMemoryStore;

    /** Whether pgvector is available */
    private boolean pgVectorAvailable = false;

    /** Configured dimension */
    private int dimension = DEFAULT_DIMENSION;

    /**
     * OSGi activate - initialize the embedding store.
     */
    @Activate
    public void activate() {
        log.info("Activating EmbeddingStoreProvider...");
        initializeStore();
    }

    /**
     * OSGi deactivate - cleanup resources.
     */
    @Deactivate
    public void deactivate() {
        log.info("Deactivating EmbeddingStoreProvider");
        pgVectorStore = null;
        inMemoryStore = null;
    }

    /**
     * Initialize the embedding store.
     *
     * <p>Attempts to create pgvector store, falls back to in-memory if unavailable.
     */
    private void initializeStore() {
        // Always create fallback
        inMemoryStore = new InMemoryEmbeddingStore<>();

        // Try to initialize pgvector
        try {
            if (checkPgVectorExtension()) {
                createPgVectorStore();
                pgVectorAvailable = true;
                log.info("EmbeddingStoreProvider initialized with pgvector, dimension=" + dimension);
            } else {
                log.warning("pgvector extension not available - using in-memory fallback");
                pgVectorAvailable = false;
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to initialize pgvector store: " + e.getMessage(), e);
            pgVectorAvailable = false;
        }
    }

    /**
     * Check if pgvector extension is installed.
     */
    private boolean checkPgVectorExtension() {
        String sql = "SELECT 1 FROM pg_extension WHERE extname = 'vector'";

        try (Connection conn = DB.getConnectionRO();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            return rs.next();

        } catch (SQLException e) {
            log.log(Level.FINE, "Could not check pgvector extension: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Create the pgvector embedding store.
     */
    private void createPgVectorStore() {
        // Get connection parameters from iDempiere's CConnection
        CConnection cc = Ini.isClient() ? CConnection.get() : CConnection.get();
        if (cc == null) {
            throw new IllegalStateException("No CConnection available");
        }

        String host = cc.getDbHost();
        int port = cc.getDbPort();
        String database = cc.getDbName();
        String user = cc.getDbUid();
        String password = cc.getDbPwd();

        if (host == null || database == null || user == null) {
            throw new IllegalStateException("Database connection parameters not available from CConnection");
        }

        // Check if table exists
        if (!checkTableExists()) {
            log.warning("Table " + TABLE_NAME + " does not exist - run migration first");
            throw new IllegalStateException("Table " + TABLE_NAME + " not found. Run migration 202512101600_P1_Vector_Embedding.sql");
        }

        // Create pgvector store using connection parameters
        pgVectorStore = PgVectorEmbeddingStore.builder()
            .host(host)
            .port(port)
            .database(database)
            .user(user)
            .password(password)
            .table(TABLE_NAME)
            .dimension(dimension)
            .createTable(false)  // Table created by migration
            .build();

        log.info("PgVectorEmbeddingStore created for table: " + TABLE_NAME + " on " + host + ":" + port);
    }

    /**
     * Check if the embedding table exists.
     */
    private boolean checkTableExists() {
        String sql = "SELECT 1 FROM information_schema.tables WHERE table_name = ?";

        try (Connection conn = DB.getConnectionRO();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, TABLE_NAME);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            log.log(Level.FINE, "Could not check table existence: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public EmbeddingStore<TextSegment> getStore() {
        if (pgVectorAvailable && pgVectorStore != null) {
            return pgVectorStore;
        }
        return inMemoryStore;
    }

    @Override
    public boolean isAvailable() {
        return pgVectorAvailable && pgVectorStore != null;
    }

    @Override
    public boolean isFallbackMode() {
        return !pgVectorAvailable;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public int clearBySourceType(String sourceType, int clientId) {
        if (!pgVectorAvailable) {
            log.warning("Cannot clear by source type - pgvector not available");
            return 0;
        }

        String sql = "DELETE FROM " + TABLE_NAME +
                    " WHERE source_type = ? AND ad_client_id = ?";

        try {
            int deleted = DB.executeUpdate(sql, new Object[] { sourceType, clientId }, false, null);
            log.info("Cleared " + deleted + " embeddings for source_type=" + sourceType +
                    ", client=" + clientId);
            return deleted;
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to clear embeddings", e);
            return 0;
        }
    }

    @Override
    public Map<String, Object> getStatistics(int clientId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pgVectorAvailable", pgVectorAvailable);
        stats.put("dimension", dimension);
        stats.put("tableName", TABLE_NAME);
        stats.put("fallbackMode", isFallbackMode());

        if (!pgVectorAvailable) {
            stats.put("totalCount", 0);
            return stats;
        }

        // Get counts by source type
        String sql = "SELECT source_type, COUNT(*) as cnt " +
                    "FROM " + TABLE_NAME + " " +
                    "WHERE ad_client_id = ? " +
                    "GROUP BY source_type";

        Map<String, Long> countsBySource = new HashMap<>();
        long totalCount = 0;

        try (Connection conn = DB.getConnectionRO();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, clientId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String sourceType = rs.getString("source_type");
                    long count = rs.getLong("cnt");
                    countsBySource.put(sourceType, count);
                    totalCount += count;
                }
            }

        } catch (SQLException e) {
            log.log(Level.WARNING, "Failed to get statistics", e);
        }

        stats.put("countsBySource", countsBySource);
        stats.put("totalCount", totalCount);

        return stats;
    }

    /**
     * Set the embedding dimension (for testing or reconfiguration).
     *
     * @param dimension New dimension value
     */
    public void setDimension(int dimension) {
        if (dimension > 0) {
            this.dimension = dimension;
        }
    }

    /**
     * Force reinitialization of the store.
     *
     * <p>Useful after database changes or configuration updates.
     */
    public void reinitialize() {
        log.info("Reinitializing EmbeddingStoreProvider...");
        pgVectorStore = null;
        pgVectorAvailable = false;
        initializeStore();
    }
}
