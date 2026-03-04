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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.langchain4j.LangChain4jProviderFactory;
import com.cloudempiere.ai.rag.embedding.IEmbeddingStoreProvider;
import com.cloudempiere.ai.util.EditorJsParser;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * Batch ingestor for K_Entry (Knowledge Base) records.
 *
 * <p>This ingestor performs bulk ingestion of existing K_Entry records
 * into the vector store. It's used for:
 * <ul>
 *   <li>Initial population of the embedding store</li>
 *   <li>Full re-indexing when forced</li>
 *   <li>Catch-up indexing for entries added before trigger was active</li>
 * </ul>
 *
 * <p>For real-time updates, the {@link com.cloudempiere.ai.event.KEntryEventHandler}
 * handles individual K_Entry changes via the trigger pattern.
 *
 * <p><b>Knowledge Base Structure:</b>
 * <pre>
 * K_Category (top-level categories)
 *   └── K_Topic (topics within category)
 *         └── K_Entry (knowledge entries with EditorJS content)
 * </pre>
 *
 * <p>Reference: ADR-016 (Knowledge Base Agent), ADR-029 (Multi-Tenant AI Access)
 *
 * @author Cloudempiere AI Team
 * @version ADR-016
 * @since v0.12.0
 */
@Component(
    service = IKnowledgeIngestor.class,
    immediate = true,
    property = {
        "service.ranking:Integer=90"  // Lower than ADMetadataIngestor (100)
    }
)
public class KnowledgeEntryIngestor implements IKnowledgeIngestor {

    private static final CLogger log = CLogger.getCLogger(KnowledgeEntryIngestor.class);

    private static final String SOURCE_TYPE = "knowledge_entry";
    private static final int DEFAULT_PROVIDER_ID = 1000001;

    @Reference
    private IEmbeddingStoreProvider storeProvider;

    @Override
    public String getSourceType() {
        return SOURCE_TYPE;
    }

    @Override
    public String getDisplayName() {
        return "Knowledge Base Entries (K_Entry)";
    }

    @Override
    public int getPriority() {
        return 20;  // After AD metadata (10)
    }

    @Override
    public IngestResult ingest(Properties ctx, boolean forceRefresh) {
        IngestResult result = new IngestResult();
        long startTime = System.currentTimeMillis();

        try {
            if (!storeProvider.isAvailable()) {
                return IngestResult.failed("Embedding store not available");
            }

            EmbeddingModel embeddingModel = getEmbeddingModel(ctx);
            if (embeddingModel == null) {
                return IngestResult.failed("Embedding model not available");
            }

            EmbeddingStore<TextSegment> store = storeProvider.getStore();
            int clientId = Env.getAD_Client_ID(ctx);

            // For knowledge entries, we may want to index from the KB client
            // This is controlled by ADR-029 multi-tenant access rules
            int targetClientId = getTargetClientId(ctx, clientId);

            // Clear existing if force refresh
            if (forceRefresh) {
                int deleted = storeProvider.clearBySourceType(SOURCE_TYPE, targetClientId);
                result.setDocumentsDeleted(deleted);
                log.info("Cleared " + deleted + " existing K_Entry embeddings for client " + targetClientId);
            }

            // Get last ingestion time for incremental updates
            Timestamp lastIngestion = forceRefresh ? null : getLastIngestion(ctx);

            // Ingest entries
            ingestEntries(ctx, store, embeddingModel, result, targetClientId, lastIngestion);

            // Update ingestion metadata
            updateIngestionMetadata(ctx, result, targetClientId);

            result.setSuccess(true);

        } catch (Exception e) {
            log.log(Level.SEVERE, "K_Entry ingestion failed", e);
            result.setErrorMessage(e.getMessage());
        }

        result.setDurationMs(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * Determine target client ID for knowledge ingestion.
     *
     * <p>Per ADR-029, knowledge is stored in a dedicated tenant (1000014)
     * but should be accessible to all users.
     */
    private int getTargetClientId(Properties ctx, int userClientId) {
        // For now, use the current user's client
        // In production, might need to check if user has access to KB client
        return userClientId;
    }

    /**
     * Ingest K_Entry records into the embedding store.
     */
    private void ingestEntries(Properties ctx, EmbeddingStore<TextSegment> store,
            EmbeddingModel embeddingModel, IngestResult result, int clientId, Timestamp since) {

        String sql = buildEntriesQuery(since);

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, null);
            int paramIdx = 1;
            pstmt.setInt(paramIdx++, clientId);
            if (since != null) {
                pstmt.setTimestamp(paramIdx++, since);
            }
            rs = pstmt.executeQuery();
            while (rs.next()) {
                try {
                    String text = buildEntryText(rs);
                    if (text == null || text.trim().isEmpty()) {
                        result.incrementSkipped();
                        continue;
                    }

                    int entryId = rs.getInt("K_Entry_ID");
                    String name = rs.getString("Name");
                    String topicName = rs.getString("TopicName");
                    String typeName = rs.getString("TypeName");

                    Metadata metadata = Metadata.from("source_type", SOURCE_TYPE)
                        .put("source_id", String.valueOf(entryId))
                        .put("source_table", "K_Entry")
                        .put("entity_type", "knowledge")
                        .put("title", name)
                        .put("ad_client_id", String.valueOf(clientId));

                    if (topicName != null) {
                        metadata.put("topic", topicName);
                    }
                    if (typeName != null) {
                        metadata.put("type", typeName);
                    }

                    TextSegment segment = TextSegment.from(text, metadata);
                    Embedding embedding = embeddingModel.embed(segment).content();
                    store.add(embedding, segment);

                    result.incrementAdded();

                    if (result.getDocumentsAdded() % 50 == 0) {
                        log.info("Ingested " + result.getDocumentsAdded() + " K_Entry records...");
                    }

                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to ingest K_Entry", e);
                    result.addError("K_Entry: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to query K_Entry records", e);
            result.addError("K_Entry query failed: " + e.getMessage());
        } finally {
            DB.close(rs, pstmt);
        }

        log.info("Ingested " + result.getDocumentsAdded() + " K_Entry records, " +
                result.getDocumentsSkipped() + " skipped");
    }

    /**
     * Build SQL query for K_Entry records.
     */
    private String buildEntriesQuery(Timestamp since) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT e.K_Entry_ID, e.Name, e.TextMsg, e.Keywords, e.Description, ");
        sql.append("       t.Name as TopicName, c.Name as CategoryName, ");
        sql.append("       kt.Name as TypeName ");
        sql.append("FROM K_Entry e ");
        sql.append("LEFT JOIN K_Topic t ON e.K_Topic_ID = t.K_Topic_ID ");
        sql.append("LEFT JOIN K_Category c ON t.K_Category_ID = c.K_Category_ID ");
        sql.append("LEFT JOIN K_Type kt ON e.K_Type_ID = kt.K_Type_ID ");
        sql.append("WHERE e.IsActive = 'Y' ");
        sql.append("  AND e.AD_Client_ID = ? ");

        if (since != null) {
            sql.append("  AND e.Updated > ? ");
        }

        sql.append("ORDER BY e.K_Entry_ID");

        return sql.toString();
    }

    /**
     * Build text content for embedding from result set.
     */
    private String buildEntryText(ResultSet rs) throws SQLException {
        StringBuilder sb = new StringBuilder();

        String name = rs.getString("Name");
        String textMsg = rs.getString("TextMsg");
        String keywords = rs.getString("Keywords");
        String description = rs.getString("Description");
        String topicName = rs.getString("TopicName");
        String categoryName = rs.getString("CategoryName");
        String typeName = rs.getString("TypeName");

        // Title
        if (name != null && !name.isEmpty()) {
            sb.append("Title: ").append(name).append("\n");
        }

        // Topic and Category context
        if (categoryName != null) {
            sb.append("Category: ").append(categoryName).append("\n");
        }
        if (topicName != null) {
            sb.append("Topic: ").append(topicName).append("\n");
        }
        if (typeName != null) {
            sb.append("Type: ").append(typeName).append("\n");
        }

        // Keywords
        if (keywords != null && !keywords.isEmpty()) {
            sb.append("Keywords: ").append(keywords).append("\n");
        }

        // Description (short summary)
        if (description != null && !description.isEmpty()) {
            sb.append("Summary: ").append(description).append("\n");
        }

        // Main content
        if (textMsg != null && !textMsg.isEmpty()) {
            String content = parseTextMsg(textMsg);
            if (content != null && !content.isEmpty()) {
                sb.append("\nContent:\n").append(content);
            }
        }

        return sb.toString().trim();
    }

    /**
     * Parse TextMsg field - handles EditorJS JSON or plain text.
     */
    private String parseTextMsg(String textMsg) {
        if (textMsg == null || textMsg.isEmpty()) {
            return null;
        }

        // Check if it's EditorJS JSON format
        String trimmed = textMsg.trim();
        if (trimmed.startsWith("{") && trimmed.contains("\"blocks\"")) {
            try {
                String markdown = EditorJsParser.parseToMarkdown(textMsg);
                if (markdown != null && !markdown.isEmpty()) {
                    return markdown;
                }
            } catch (Exception e) {
                log.log(Level.FINE, "EditorJS parsing failed, using raw text", e);
            }
        }

        return textMsg;
    }

    /**
     * Update ingestion metadata table.
     */
    private void updateIngestionMetadata(Properties ctx, IngestResult result, int clientId) {
        String upsertSql = "INSERT INTO aig_ingestion_metadata " +
                    "(ad_client_id, source_type, last_ingestion, last_successful_ingestion, " +
                    "record_count, status, duration_ms, created, updated) " +
                    "VALUES (?, ?, NOW(), ?, ?, ?, ?, NOW(), NOW()) " +
                    "ON CONFLICT (ad_client_id, source_type) " +
                    "DO UPDATE SET " +
                    "last_ingestion = NOW(), " +
                    "last_successful_ingestion = CASE WHEN EXCLUDED.status = 'completed' THEN NOW() ELSE aig_ingestion_metadata.last_successful_ingestion END, " +
                    "record_count = EXCLUDED.record_count, " +
                    "status = EXCLUDED.status, " +
                    "duration_ms = EXCLUDED.duration_ms, " +
                    "error_message = EXCLUDED.error_message, " +
                    "updated = NOW()";

        try {
            DB.executeUpdate(upsertSql, new Object[] {
                clientId,
                SOURCE_TYPE,
                result.isSuccess() ? new Timestamp(System.currentTimeMillis()) : null,
                result.getTotalProcessed(),
                result.isSuccess() ? "completed" : "failed",
                result.getDurationMs()
            }, false, null);
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to update ingestion metadata", e);
        }
    }

    @Override
    public boolean needsRefresh(Properties ctx) {
        Timestamp lastIngestion = getLastIngestion(ctx);
        if (lastIngestion == null) {
            return true;
        }

        int clientId = Env.getAD_Client_ID(ctx);

        // Check if any K_Entry was updated since last ingestion
        String sql = "SELECT MAX(Updated) FROM K_Entry WHERE IsActive = 'Y' AND AD_Client_ID = ?";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, null);
            pstmt.setInt(1, clientId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                Timestamp lastUpdate = rs.getTimestamp(1);
                return lastUpdate != null && lastUpdate.after(lastIngestion);
            }
        } catch (SQLException e) {
            log.log(Level.WARNING, "Failed to check refresh status", e);
        } finally {
            DB.close(rs, pstmt);
        }

        return true;  // Default to refresh if check fails
    }

    @Override
    public Timestamp getLastIngestion(Properties ctx) {
        int clientId = Env.getAD_Client_ID(ctx);

        String sql = "SELECT last_successful_ingestion FROM aig_ingestion_metadata " +
                    "WHERE ad_client_id = ? AND source_type = ?";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, null);
            pstmt.setInt(1, clientId);
            pstmt.setString(2, SOURCE_TYPE);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getTimestamp("last_successful_ingestion");
            }
        } catch (SQLException e) {
            log.log(Level.FINE, "Failed to get last ingestion", e);
        } finally {
            DB.close(rs, pstmt);
        }

        return null;
    }

    @Override
    public int getEstimatedCount(Properties ctx) {
        int clientId = Env.getAD_Client_ID(ctx);

        String sql = "SELECT COUNT(*) FROM K_Entry WHERE IsActive = 'Y' AND AD_Client_ID = ?";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, null);
            pstmt.setInt(1, clientId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.log(Level.FINE, "Failed to get estimated count", e);
        } finally {
            DB.close(rs, pstmt);
        }

        return 0;
    }

    /**
     * Get embedding model from provider.
     */
    private EmbeddingModel getEmbeddingModel(Properties ctx) {
        try {
            MAIProvider provider = MAIProvider.get(ctx, DEFAULT_PROVIDER_ID, null);
            if (provider != null) {
                return LangChain4jProviderFactory.createEmbeddingModel(provider);
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to create embedding model", e);
        }
        return null;
    }
}
