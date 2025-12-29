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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import org.compiere.db.CConnection;
import org.compiere.model.MIssue;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Ini;

import com.cloudempiere.ai.model.MAIProvider;
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
 *   <li>Lazy initialization - waits for database to be available</li>
 *   <li>Configurable initialization mode (strict vs graceful)</li>
 *   <li>Automatic fallback to in-memory if pgvector unavailable (graceful mode)</li>
 *   <li>Connection parameters from iDempiere's CConnection</li>
 *   <li>Multi-tenant aware (filtering by AD_Client_ID)</li>
 *   <li>HNSW index for fast similarity search</li>
 * </ul>
 *
 * <p>Configuration:
 * <ul>
 *   <li>Table: aig_embedding (created by migration 202512101600_CLD-1601.sql)</li>
 *   <li>Dimension: 768 (nomic-embed-text default)</li>
 *   <li>Index: HNSW with cosine distance</li>
 *   <li>System property: {@code ai.embedding.mode} - "strict" or "graceful" (default)</li>
 * </ul>
 *
 * <p>Initialization Modes:
 * <ul>
 *   <li><b>graceful</b> (default): Falls back to in-memory store if prerequisites missing</li>
 *   <li><b>strict</b>: Throws exception if prerequisites missing (for production)</li>
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

    /** System property to control initialization mode */
    public static final String PROP_INIT_MODE = "ai.embedding.mode";

    /** Strict mode value - fail if prerequisites missing */
    public static final String MODE_STRICT = "strict";

    /** Graceful mode value - fallback to in-memory if prerequisites missing */
    public static final String MODE_GRACEFUL = "graceful";

    /**
     * Initialization mode for embedding store.
     */
    public enum InitMode {
        /** Fail fast if prerequisites are missing (for production) */
        STRICT,
        /** Fallback to in-memory if prerequisites missing (for dev/testing) */
        GRACEFUL
    }

    /**
     * Result of prerequisite check with detailed status.
     */
    public static class PrerequisiteStatus {
        private final boolean databaseAvailable;
        private final boolean pgVectorInstalled;
        private final boolean tableExists;
        private final List<String> missingPrerequisites;

        public PrerequisiteStatus(boolean dbAvailable, boolean pgVector, boolean table) {
            this.databaseAvailable = dbAvailable;
            this.pgVectorInstalled = pgVector;
            this.tableExists = table;
            this.missingPrerequisites = new ArrayList<>();
            if (!dbAvailable) missingPrerequisites.add("Database connection not available");
            if (!pgVector) missingPrerequisites.add("pgvector extension not installed");
            if (!table) missingPrerequisites.add("Table '" + TABLE_NAME + "' not created (run migration)");
        }

        public boolean isAllMet() {
            return databaseAvailable && pgVectorInstalled && tableExists;
        }

        public boolean isDatabaseAvailable() { return databaseAvailable; }
        public boolean isPgVectorInstalled() { return pgVectorInstalled; }
        public boolean isTableExists() { return tableExists; }
        public List<String> getMissingPrerequisites() { return missingPrerequisites; }

        @Override
        public String toString() {
            if (isAllMet()) {
                return "All prerequisites met";
            }
            return "Missing prerequisites: " + String.join(", ", missingPrerequisites);
        }
    }

    /** pgvector embedding store (null if unavailable) */
    private volatile PgVectorEmbeddingStore pgVectorStore;

    /** Fallback in-memory store */
    private volatile InMemoryEmbeddingStore<TextSegment> inMemoryStore;

    /** Whether pgvector is available */
    private volatile boolean pgVectorAvailable = false;

    /** Whether initialization has been attempted */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /** Lock for thread-safe lazy initialization */
    private final Object initLock = new Object();

    /** Configured dimension */
    private int dimension = DEFAULT_DIMENSION;

    /** Current initialization mode */
    private InitMode initMode = InitMode.GRACEFUL;

    /** Last prerequisite check result */
    private volatile PrerequisiteStatus lastPrerequisiteStatus;

    /** Flag to prevent creating multiple AD_Issue records */
    private final AtomicBoolean issueCreated = new AtomicBoolean(false);

    /**
     * OSGi activate - defer initialization until database is available.
     *
     * <p>Lazy initialization pattern: we don't attempt database connection
     * during OSGi bundle activation because iDempiere's database subsystem
     * may not be ready yet. Instead, we initialize on first use.
     */
    @Activate
    public void activate() {
        // Read initialization mode from system property
        String modeStr = System.getProperty(PROP_INIT_MODE, MODE_GRACEFUL);
        if (MODE_STRICT.equalsIgnoreCase(modeStr)) {
            initMode = InitMode.STRICT;
            log.info("EmbeddingStoreProvider registered in STRICT mode (will fail if prerequisites missing)");
        } else {
            initMode = InitMode.GRACEFUL;
            log.info("EmbeddingStoreProvider registered in GRACEFUL mode (will fallback to in-memory if needed)");
        }
    }

    /**
     * OSGi deactivate - cleanup resources.
     */
    @Deactivate
    public void deactivate() {
        log.info("Deactivating EmbeddingStoreProvider");
        synchronized (initLock) {
            pgVectorStore = null;
            inMemoryStore = null;
            pgVectorAvailable = false;
            initialized.set(false);
            issueCreated.set(false);
        }
    }

    /**
     * Ensure the store is initialized (lazy initialization).
     *
     * <p>Thread-safe double-checked locking pattern ensures initialization
     * happens exactly once, on first access.
     */
    private void ensureInitialized() {
        if (!initialized.get()) {
            synchronized (initLock) {
                if (!initialized.get()) {
                    initializeStore();
                    initialized.set(true);
                }
            }
        }
    }

    /**
     * Check if the database connection is available.
     *
     * @return true if database is ready for connections
     */
    private boolean isDatabaseAvailable() {
        try {
            Connection conn = DB.getConnectionRO();
            if (conn != null) {
                conn.close();
                return true;
            }
        } catch (Exception e) {
            log.log(Level.FINE, "Database not yet available: " + e.getMessage());
        }
        return false;
    }

    /**
     * Check all prerequisites and return detailed status.
     *
     * <p>This method can be called externally to diagnose setup issues.
     *
     * @return PrerequisiteStatus with detailed information about each prerequisite
     */
    public PrerequisiteStatus checkPrerequisites() {
        boolean dbAvailable = isDatabaseAvailable();
        boolean pgVector = dbAvailable && checkPgVectorExtension();
        boolean table = dbAvailable && checkTableExists();

        lastPrerequisiteStatus = new PrerequisiteStatus(dbAvailable, pgVector, table);
        return lastPrerequisiteStatus;
    }

    /**
     * Get the last prerequisite check result.
     *
     * @return last PrerequisiteStatus or null if never checked
     */
    public PrerequisiteStatus getLastPrerequisiteStatus() {
        return lastPrerequisiteStatus;
    }

    /**
     * Initialize the embedding store.
     *
     * <p>Behavior depends on initialization mode:
     * <ul>
     *   <li>GRACEFUL: Falls back to in-memory if prerequisites missing</li>
     *   <li>STRICT: Throws exception if prerequisites missing</li>
     * </ul>
     */
    private void initializeStore() {
        // Always create fallback first (needed for graceful mode)
        inMemoryStore = new InMemoryEmbeddingStore<>();

        // Check all prerequisites
        PrerequisiteStatus status = checkPrerequisites();

        if (!status.isDatabaseAvailable()) {
            handlePrerequisiteFailure("Database not available", status);
            initialized.set(false);  // Allow retry on next access
            return;
        }

        if (!status.isPgVectorInstalled()) {
            handlePrerequisiteFailure("pgvector extension not installed", status);
            return;
        }

        if (!status.isTableExists()) {
            handlePrerequisiteFailure("Table '" + TABLE_NAME + "' not created - run migration first", status);
            return;
        }

        // All prerequisites met - create pgvector store
        try {
            createPgVectorStore();
            pgVectorAvailable = true;
            log.info("EmbeddingStoreProvider initialized with pgvector, dimension=" + dimension);
        } catch (Throwable e) {
            // Catch Throwable (not just Exception) to handle NoClassDefFoundError
            // when PostgreSQL JDBC driver is not available in classpath
            handlePrerequisiteFailure("Failed to create pgvector store: " + e.getMessage(), status);
        }
    }

    /**
     * Handle prerequisite failure based on initialization mode.
     *
     * @param message Error message
     * @param status Prerequisite status for detailed logging
     */
    private void handlePrerequisiteFailure(String message, PrerequisiteStatus status) {
        pgVectorAvailable = false;

        // Create AD_Issue to notify admin (only once per session)
        createAdminIssue(message, status);

        if (initMode == InitMode.STRICT) {
            String errorMsg = "STRICT MODE: " + message + ". " + status.toString();
            log.log(Level.SEVERE, errorMsg);
            throw new IllegalStateException(errorMsg);
        } else {
            log.warning(message + " - using in-memory fallback. " + status.toString());
        }
    }

    /**
     * Create an AD_Issue record to notify administrators about missing prerequisites.
     *
     * <p>Only creates one issue per session to avoid flooding the issue list.
     *
     * @param message Summary message
     * @param status Detailed prerequisite status
     */
    private void createAdminIssue(String message, PrerequisiteStatus status) {
        // Only create issue once per session
        if (!issueCreated.compareAndSet(false, true)) {
            return;
        }

        // Only create issue if database is available
        if (!status.isDatabaseAvailable()) {
            log.fine("Cannot create AD_Issue - database not available");
            return;
        }

        try {
            // Get client from AIG_Provider - issue goes to same tenant as provider
            int clientId = getClientIdFromProvider();

            // No provider configured = no issue needed (nothing to notify about)
            if (clientId < 0) {
                log.fine("No AIG_Provider configured - skipping AD_Issue creation");
                issueCreated.set(false);  // Allow retry when provider is created
                return;
            }

            Properties ctx = Env.getCtx();
            int originalClientId = Env.getAD_Client_ID(ctx);
            int originalOrgId = Env.getAD_Org_ID(ctx);

            try {
                // Set context to match provider's tenant
                Env.setContext(ctx, Env.AD_CLIENT_ID, clientId);
                Env.setContext(ctx, Env.AD_ORG_ID, 0);  // Org=0 for visibility

                createIssueRecord(message, status, ctx);
            } finally {
                // Restore original context
                Env.setContext(ctx, Env.AD_CLIENT_ID, originalClientId);
                Env.setContext(ctx, Env.AD_ORG_ID, originalOrgId);
            }
        } catch (Exception e) {
            // Don't fail the main flow if issue creation fails
            log.log(Level.FINE, "Could not create AD_Issue for admin notification: " + e.getMessage(), e);
        }
    }

    /**
     * Get AD_Client_ID from AIG_Provider configuration.
     *
     * <p>Finds the first active AI provider to determine which client
     * should receive the initialization issue notification.
     * Uses iDempiere's Query API which integrates with caching.
     *
     * @return AD_Client_ID from provider (can be 0 for System), or -1 if none found
     */
    private int getClientIdFromProvider() {
        try {
            // Use Query API - integrates with iDempiere caching
            MAIProvider provider = new Query(Env.getCtx(), MAIProvider.Table_Name, "IsActive='Y'", null)
                .setOnlyActiveRecords(true)
                .setOrderBy("AD_Client_ID")
                .first();

            if (provider != null) {
                return provider.getAD_Client_ID();
            }
        } catch (Exception e) {
            log.log(Level.FINE, "Could not query AIG_Provider for client: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Create the actual AD_Issue record.
     */
    private void createIssueRecord(String message, PrerequisiteStatus status, Properties ctx) {
        try {
            MIssue issue = new MIssue(ctx, 0, null);
            issue.setIssueSummary("AI Plugin: Vector Database Not Initialized");
            issue.setSourceClassName(EmbeddingStoreProvider.class.getName());
            issue.setSourceMethodName("initializeStore");
            issue.setLoggerName(log.getName());

            // Build detailed description
            StringBuilder description = new StringBuilder();
            description.append("The AI plugin's vector embedding store could not be initialized.\n\n");
            description.append("Mode: ").append(initMode.name()).append("\n\n");
            description.append("Issue: ").append(message).append("\n\n");
            description.append("Prerequisites Status:\n");
            description.append("- Database Available: ").append(status.isDatabaseAvailable() ? "YES" : "NO").append("\n");
            description.append("- pgvector Extension: ").append(status.isPgVectorInstalled() ? "INSTALLED" : "NOT INSTALLED").append("\n");
            description.append("- Table '").append(TABLE_NAME).append("': ").append(status.isTableExists() ? "EXISTS" : "NOT CREATED").append("\n");

            if (!status.getMissingPrerequisites().isEmpty()) {
                description.append("\nMissing Prerequisites:\n");
                for (String missing : status.getMissingPrerequisites()) {
                    description.append("  - ").append(missing).append("\n");
                }
            }

            description.append("\nResolution Steps:\n");
            if (!status.isPgVectorInstalled()) {
                description.append("1. Install pgvector extension in PostgreSQL:\n");
                description.append("   CREATE EXTENSION IF NOT EXISTS vector;\n\n");
            }
            if (!status.isTableExists()) {
                description.append("2. Run the migration script:\n");
                description.append("   202512101600_P1_Vector_Embedding.sql\n\n");
            }
            description.append("After fixing, restart the server or call EmbeddingStoreProvider.reinitialize()");

            issue.setStackTrace(description.toString());
            issue.setLogLevel(initMode == InitMode.STRICT ? "SEVERE" : "WARNING");
            issue.saveEx();

            log.info("Created AD_Issue #" + issue.getAD_Issue_ID() + " for vector database initialization failure");

        } catch (Exception e) {
            // Don't fail the main flow if issue creation fails
            log.log(Level.FINE, "Could not create AD_Issue for admin notification: " + e.getMessage(), e);
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
        ensureInitialized();
        if (pgVectorAvailable && pgVectorStore != null) {
            return pgVectorStore;
        }
        // Ensure fallback is available even if init failed
        if (inMemoryStore == null) {
            inMemoryStore = new InMemoryEmbeddingStore<>();
        }
        return inMemoryStore;
    }

    @Override
    public boolean isAvailable() {
        ensureInitialized();
        return pgVectorAvailable && pgVectorStore != null;
    }

    @Override
    public boolean isFallbackMode() {
        ensureInitialized();
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
        ensureInitialized();
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
        ensureInitialized();
        Map<String, Object> stats = new HashMap<>();
        stats.put("pgVectorAvailable", pgVectorAvailable);
        stats.put("dimension", dimension);
        stats.put("tableName", TABLE_NAME);
        stats.put("fallbackMode", !pgVectorAvailable);
        stats.put("initialized", initialized.get());
        stats.put("initMode", initMode.name());

        // Include prerequisite status if available
        if (lastPrerequisiteStatus != null) {
            Map<String, Object> prereqs = new HashMap<>();
            prereqs.put("databaseAvailable", lastPrerequisiteStatus.isDatabaseAvailable());
            prereqs.put("pgVectorInstalled", lastPrerequisiteStatus.isPgVectorInstalled());
            prereqs.put("tableExists", lastPrerequisiteStatus.isTableExists());
            prereqs.put("allMet", lastPrerequisiteStatus.isAllMet());
            prereqs.put("missing", lastPrerequisiteStatus.getMissingPrerequisites());
            stats.put("prerequisites", prereqs);
        }

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
     * Get current initialization mode.
     *
     * @return current InitMode
     */
    public InitMode getInitMode() {
        return initMode;
    }

    /**
     * Set initialization mode programmatically.
     *
     * <p>This allows runtime switching between STRICT and GRACEFUL modes.
     * Call {@link #reinitialize()} after changing mode to apply it.
     *
     * @param mode new initialization mode
     */
    public void setInitMode(InitMode mode) {
        if (mode != null) {
            this.initMode = mode;
            log.info("Initialization mode set to: " + mode);
        }
    }

    /**
     * Force reinitialization of the store.
     *
     * <p>Useful after database changes, configuration updates, or when
     * prerequisites become available (e.g., migration creates table).
     */
    public void reinitialize() {
        log.info("Reinitializing EmbeddingStoreProvider...");
        synchronized (initLock) {
            pgVectorStore = null;
            pgVectorAvailable = false;
            initialized.set(false);
            issueCreated.set(false);  // Allow new issue if reinit fails
            initializeStore();
            initialized.set(true);
        }
    }

    /**
     * Check if initialization has been completed.
     *
     * @return true if initialization has been attempted
     */
    public boolean isInitialized() {
        return initialized.get();
    }
}
