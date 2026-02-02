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

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.langchain4j.LangChain4jProviderFactory;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * Service for trigger-based embedding operations.
 *
 * <p>Provides operations for adding, updating, and deleting embeddings
 * in response to iDempiere PO events. This service is called by event handlers
 * when K_Entry or other embeddable entities change.
 *
 * <p>Key features:
 * <ul>
 *   <li>Synchronous embedding on entity change (not async queue)</li>
 *   <li>Content hash-based deduplication (avoids re-embedding unchanged content)</li>
 *   <li>Multi-tenant aware (AD_Client_ID in metadata)</li>
 *   <li>Source tracking (source_type, source_id, source_table)</li>
 * </ul>
 *
 * <p>Reference: ADR-029 (Push to Vector on K_Entry Change pattern)
 *
 * @author Cloudempiere AI Team
 * @version ADR-029
 * @since v0.12.0
 */
@Component(
    service = IEmbeddingTriggerService.class,
    immediate = true,
    property = {
        "service.ranking:Integer=100"
    }
)
public class EmbeddingTriggerService implements IEmbeddingTriggerService {

    private static final CLogger log = CLogger.getCLogger(EmbeddingTriggerService.class);

    /** Default provider ID for embedding model */
    private static final int DEFAULT_PROVIDER_ID = 1000001;

    /** Source type for knowledge entries */
    public static final String SOURCE_TYPE_KNOWLEDGE_ENTRY = "knowledge_entry";

    @Reference
    private IEmbeddingStoreProvider storeProvider;

    /** Cached embedding model */
    private EmbeddingModel embeddingModel;

    /** Lock for model initialization */
    private final Object modelLock = new Object();

    @Activate
    public void activate() {
        log.info("EmbeddingTriggerService activated");
    }

    @Deactivate
    public void deactivate() {
        log.info("EmbeddingTriggerService deactivated");
        embeddingModel = null;
    }

    @Override
    public EmbeddingResult addOrUpdateEmbedding(Properties ctx, String sourceType, String sourceId,
            String sourceTable, String textContent, String title, String language) {

        long startTime = System.currentTimeMillis();

        if (!isAvailable()) {
            return EmbeddingResult.failed("Embedding service not available");
        }

        if (textContent == null || textContent.trim().isEmpty()) {
            return EmbeddingResult.skipped("Empty content - nothing to embed");
        }

        try {
            int clientId = Env.getAD_Client_ID(ctx);
            int orgId = Env.getAD_Org_ID(ctx);
            int userId = Env.getAD_User_ID(ctx);

            // Calculate content hash for deduplication
            String contentHash = calculateHash(textContent);

            // Check if embedding with same hash already exists
            String existingId = findBySourceAndHash(sourceType, sourceId, clientId, contentHash);
            if (existingId != null) {
                log.fine("Embedding unchanged (hash match) for " + sourceType + "/" + sourceId);
                return EmbeddingResult.skipped("Content unchanged");
            }

            // Delete existing embedding for this source (if any)
            int deleted = deleteBySource(sourceType, sourceId, clientId);
            if (deleted > 0) {
                log.fine("Deleted " + deleted + " existing embedding(s) for " + sourceType + "/" + sourceId);
            }

            // Get embedding model
            EmbeddingModel model = getEmbeddingModel(ctx);
            if (model == null) {
                return EmbeddingResult.failed("Embedding model not available");
            }

            // Build metadata
            Metadata metadata = Metadata.from("source_type", sourceType)
                .put("source_id", sourceId)
                .put("source_table", sourceTable)
                .put("ad_client_id", String.valueOf(clientId))
                .put("ad_org_id", String.valueOf(orgId))
                .put("createdby", String.valueOf(userId));

            if (title != null && !title.isEmpty()) {
                metadata.put("title", title);
            }
            if (language != null && !language.isEmpty()) {
                metadata.put("ad_language", language);
            }

            // Create segment and generate embedding
            TextSegment segment = TextSegment.from(textContent, metadata);
            Embedding embedding = model.embed(segment).content();

            // Store embedding
            EmbeddingStore<TextSegment> store = storeProvider.getStore();
            String embeddingId = store.add(embedding, segment);

            // Update hash in database for future deduplication
            updateContentHash(embeddingId, contentHash, clientId);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Added embedding for " + sourceType + "/" + sourceId +
                    " [" + duration + "ms, " + textContent.length() + " chars]");

            return EmbeddingResult.success(embeddingId, deleted > 0 ? "updated" : "added", duration);

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to add embedding for " + sourceType + "/" + sourceId, e);
            return EmbeddingResult.failed("Embedding failed: " + e.getMessage());
        }
    }

    @Override
    public int deleteEmbedding(Properties ctx, String sourceType, String sourceId) {
        if (!isAvailable()) {
            log.warning("Cannot delete embedding - service not available");
            return 0;
        }

        int clientId = Env.getAD_Client_ID(ctx);
        int deleted = deleteBySource(sourceType, sourceId, clientId);

        if (deleted > 0) {
            log.info("Deleted " + deleted + " embedding(s) for " + sourceType + "/" + sourceId);
        }

        return deleted;
    }

    @Override
    public boolean hasEmbedding(Properties ctx, String sourceType, String sourceId) {
        if (!storeProvider.isAvailable()) {
            return false;
        }

        int clientId = Env.getAD_Client_ID(ctx);
        String sql = "SELECT 1 FROM " + storeProvider.getTableName() +
                    " WHERE source_type = ? AND source_id = ? AND ad_client_id = ? LIMIT 1";

        try (Connection conn = DB.getConnectionRO();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sourceType);
            pstmt.setString(2, sourceId);
            pstmt.setInt(3, clientId);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.log(Level.WARNING, "Failed to check embedding existence", e);
            return false;
        }
    }

    @Override
    public boolean isAvailable() {
        return storeProvider != null && storeProvider.isAvailable();
    }

    /**
     * Calculate SHA-256 hash of content for deduplication.
     */
    private String calculateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.log(Level.WARNING, "Hash calculation failed", e);
            return null;
        }
    }

    /**
     * Find existing embedding by source and content hash.
     */
    private String findBySourceAndHash(String sourceType, String sourceId, int clientId, String contentHash) {
        if (contentHash == null) return null;

        String sql = "SELECT aig_embedding_uu FROM " + storeProvider.getTableName() +
                    " WHERE source_type = ? AND source_id = ? AND ad_client_id = ? AND text_segment_hash = ?";

        try (Connection conn = DB.getConnectionRO();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, sourceType);
            pstmt.setString(2, sourceId);
            pstmt.setInt(3, clientId);
            pstmt.setString(4, contentHash);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException e) {
            log.log(Level.FINE, "Failed to find embedding by hash", e);
        }
        return null;
    }

    /**
     * Delete embeddings by source type and ID.
     */
    private int deleteBySource(String sourceType, String sourceId, int clientId) {
        String sql = "DELETE FROM " + storeProvider.getTableName() +
                    " WHERE source_type = ? AND source_id = ? AND ad_client_id = ?";

        try {
            return DB.executeUpdate(sql, new Object[] { sourceType, sourceId, clientId }, false, null);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to delete embeddings", e);
            return 0;
        }
    }

    /**
     * Update content hash for an embedding (for future deduplication).
     */
    private void updateContentHash(String embeddingId, String contentHash, int clientId) {
        if (embeddingId == null || contentHash == null) return;

        String sql = "UPDATE " + storeProvider.getTableName() +
                    " SET text_segment_hash = ? WHERE aig_embedding_uu = ?::uuid";

        try {
            DB.executeUpdate(sql, new Object[] { contentHash, embeddingId }, false, null);
        } catch (Exception e) {
            // Non-critical - log and continue
            log.log(Level.FINE, "Failed to update content hash", e);
        }
    }

    /**
     * Get or create embedding model.
     */
    private EmbeddingModel getEmbeddingModel(Properties ctx) {
        if (embeddingModel != null) {
            return embeddingModel;
        }

        synchronized (modelLock) {
            if (embeddingModel == null) {
                try {
                    MAIProvider provider = MAIProvider.get(ctx, DEFAULT_PROVIDER_ID, null);
                    if (provider != null) {
                        embeddingModel = LangChain4jProviderFactory.createEmbeddingModel(provider);
                        log.info("Created embedding model for trigger service from provider: " + provider.getName());
                    }
                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to create embedding model", e);
                }
            }
        }

        return embeddingModel;
    }
}
