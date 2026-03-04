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

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * Ingests Application Dictionary metadata for semantic search.
 *
 * <p>Sources:
 * <ul>
 *   <li>AD_Window (name, description, help)</li>
 *   <li>AD_Tab (name, description, help)</li>
 *   <li>AD_Process (name, description, help)</li>
 *   <li>AD_Table (name, description)</li>
 * </ul>
 *
 * <p>This enables the AI agent to answer questions like:
 * <ul>
 *   <li>"What window do I use for entering orders?"</li>
 *   <li>"How do I run the invoice generation process?"</li>
 *   <li>"What table stores customer information?"</li>
 * </ul>
 *
 * <p>Reference: idempiere-cli ADMetadataIngestor
 *
 * @author Cloudempiere AI Team
 * @version ADR-012
 * @since v0.11.0
 */
@Component(
    service = IKnowledgeIngestor.class,
    immediate = true,
    property = {
        "service.ranking:Integer=100"
    }
)
public class ADMetadataIngestor implements IKnowledgeIngestor {

    private static final CLogger log = CLogger.getCLogger(ADMetadataIngestor.class);

    private static final String SOURCE_TYPE = "ad_metadata";
    private static final int DEFAULT_PROVIDER_ID = 1000001;

    @Reference
    private IEmbeddingStoreProvider storeProvider;

    @Override
    public String getSourceType() {
        return SOURCE_TYPE;
    }

    @Override
    public String getDisplayName() {
        return "Application Dictionary Metadata";
    }

    @Override
    public int getPriority() {
        return 10;  // High priority - fundamental knowledge
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

            // Clear existing if force refresh
            if (forceRefresh) {
                int deleted = storeProvider.clearBySourceType(SOURCE_TYPE, clientId);
                result.setDocumentsDeleted(deleted);
                log.info("Cleared " + deleted + " existing AD metadata embeddings");
            }

            // Ingest each type
            ingestWindows(ctx, store, embeddingModel, result);
            ingestProcesses(ctx, store, embeddingModel, result);
            ingestTables(ctx, store, embeddingModel, result);

            // Update ingestion metadata
            updateIngestionMetadata(ctx, result);

            result.setSuccess(true);

        } catch (Exception e) {
            log.log(Level.SEVERE, "AD metadata ingestion failed", e);
            result.setErrorMessage(e.getMessage());
        }

        result.setDurationMs(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * Ingest AD_Window metadata.
     */
    private void ingestWindows(Properties ctx, EmbeddingStore<TextSegment> store,
                               EmbeddingModel embeddingModel, IngestResult result) {

        String sql = "SELECT w.AD_Window_ID, w.Name, w.Description, w.Help, " +
                    "t.Name as TabName, t.Description as TabDesc, " +
                    "tbl.TableName, " +
                    "COALESCE(wt.Name, w.Name) as TrlName, " +
                    "COALESCE(wt.Description, w.Description) as TrlDesc, " +
                    "COALESCE(wt.Help, w.Help) as TrlHelp " +
                    "FROM AD_Window w " +
                    "LEFT JOIN AD_Window_Trl wt ON w.AD_Window_ID = wt.AD_Window_ID AND wt.AD_Language = ? " +
                    "LEFT JOIN AD_Tab t ON w.AD_Window_ID = t.AD_Window_ID AND t.SeqNo = 10 AND t.IsActive = 'Y' " +
                    "LEFT JOIN AD_Table tbl ON t.AD_Table_ID = tbl.AD_Table_ID " +
                    "WHERE w.IsActive = 'Y' " +
                    "AND (w.AD_Client_ID = 0 OR w.AD_Client_ID = ?) " +
                    "ORDER BY w.Name";

        int clientId = Env.getAD_Client_ID(ctx);
        String language = Env.getAD_Language(ctx);

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, null);
            pstmt.setString(1, language);
            pstmt.setInt(2, clientId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                try {
                    String text = buildWindowText(rs);
                    if (text == null || text.trim().isEmpty()) {
                        result.incrementSkipped();
                        continue;
                    }

                    int windowId = rs.getInt("AD_Window_ID");
                    String name = rs.getString("TrlName");

                    Metadata metadata = Metadata.from("source_type", SOURCE_TYPE)
                        .put("source_id", String.valueOf(windowId))
                        .put("source_table", "AD_Window")
                        .put("entity_type", "window")
                        .put("title", name)
                        .put("ad_client_id", String.valueOf(clientId));

                    TextSegment segment = TextSegment.from(text, metadata);
                    Embedding embedding = embeddingModel.embed(segment).content();
                    store.add(embedding, segment);

                    result.incrementAdded();

                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to ingest window", e);
                    result.addError("Window: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to query windows", e);
            result.addError("Windows query failed: " + e.getMessage());
        } finally {
            DB.close(rs, pstmt);
        }

        log.info("Ingested " + result.getDocumentsAdded() + " windows");
    }

    /**
     * Build text content for window embedding.
     */
    private String buildWindowText(ResultSet rs) throws SQLException {
        StringBuilder sb = new StringBuilder();

        String name = rs.getString("TrlName");
        String desc = rs.getString("TrlDesc");
        String help = rs.getString("TrlHelp");
        String tabName = rs.getString("TabName");
        String tableName = rs.getString("TableName");

        sb.append("Window: ").append(name).append("\n");

        if (desc != null && !desc.trim().isEmpty()) {
            sb.append("Description: ").append(desc).append("\n");
        }

        if (help != null && !help.trim().isEmpty()) {
            sb.append("Help: ").append(help).append("\n");
        }

        if (tabName != null) {
            sb.append("Main Tab: ").append(tabName).append("\n");
        }

        if (tableName != null) {
            sb.append("Table: ").append(tableName).append("\n");
        }

        return sb.toString();
    }

    /**
     * Ingest AD_Process metadata.
     */
    private void ingestProcesses(Properties ctx, EmbeddingStore<TextSegment> store,
                                  EmbeddingModel embeddingModel, IngestResult result) {

        String sql = "SELECT p.AD_Process_ID, p.Value, p.Name, p.Description, p.Help, " +
                    "p.ProcedureName, p.Classname, " +
                    "COALESCE(pt.Name, p.Name) as TrlName, " +
                    "COALESCE(pt.Description, p.Description) as TrlDesc, " +
                    "COALESCE(pt.Help, p.Help) as TrlHelp " +
                    "FROM AD_Process p " +
                    "LEFT JOIN AD_Process_Trl pt ON p.AD_Process_ID = pt.AD_Process_ID AND pt.AD_Language = ? " +
                    "WHERE p.IsActive = 'Y' AND p.IsReport = 'N' " +
                    "AND (p.AD_Client_ID = 0 OR p.AD_Client_ID = ?) " +
                    "ORDER BY p.Name";

        int clientId = Env.getAD_Client_ID(ctx);
        String language = Env.getAD_Language(ctx);
        int addedBefore = result.getDocumentsAdded();

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, null);
            pstmt.setString(1, language);
            pstmt.setInt(2, clientId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                try {
                    String text = buildProcessText(rs);
                    if (text == null || text.trim().isEmpty()) {
                        result.incrementSkipped();
                        continue;
                    }

                    int processId = rs.getInt("AD_Process_ID");
                    String name = rs.getString("TrlName");

                    Metadata metadata = Metadata.from("source_type", SOURCE_TYPE)
                        .put("source_id", String.valueOf(processId))
                        .put("source_table", "AD_Process")
                        .put("entity_type", "process")
                        .put("title", name)
                        .put("ad_client_id", String.valueOf(clientId));

                    TextSegment segment = TextSegment.from(text, metadata);
                    Embedding embedding = embeddingModel.embed(segment).content();
                    store.add(embedding, segment);

                    result.incrementAdded();

                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to ingest process", e);
                    result.addError("Process: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to query processes", e);
            result.addError("Processes query failed: " + e.getMessage());
        } finally {
            DB.close(rs, pstmt);
        }

        log.info("Ingested " + (result.getDocumentsAdded() - addedBefore) + " processes");
    }

    /**
     * Build text content for process embedding.
     */
    private String buildProcessText(ResultSet rs) throws SQLException {
        StringBuilder sb = new StringBuilder();

        String name = rs.getString("TrlName");
        String value = rs.getString("Value");
        String desc = rs.getString("TrlDesc");
        String help = rs.getString("TrlHelp");

        sb.append("Process: ").append(name).append("\n");
        sb.append("Code: ").append(value).append("\n");

        if (desc != null && !desc.trim().isEmpty()) {
            sb.append("Description: ").append(desc).append("\n");
        }

        if (help != null && !help.trim().isEmpty()) {
            sb.append("Help: ").append(help).append("\n");
        }

        return sb.toString();
    }

    /**
     * Ingest AD_Table metadata.
     */
    private void ingestTables(Properties ctx, EmbeddingStore<TextSegment> store,
                              EmbeddingModel embeddingModel, IngestResult result) {

        String sql = "SELECT t.AD_Table_ID, t.TableName, t.Name, t.Description, " +
                    "(SELECT COUNT(*) FROM AD_Column c WHERE c.AD_Table_ID = t.AD_Table_ID AND c.IsActive = 'Y') as ColumnCount " +
                    "FROM AD_Table t " +
                    "WHERE t.IsActive = 'Y' AND t.IsView = 'N' " +
                    "AND (t.AD_Client_ID = 0 OR t.AD_Client_ID = ?) " +
                    "ORDER BY t.TableName";

        int clientId = Env.getAD_Client_ID(ctx);
        int addedBefore = result.getDocumentsAdded();

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, null);
            pstmt.setInt(1, clientId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                try {
                    String text = buildTableText(rs);
                    if (text == null || text.trim().isEmpty()) {
                        result.incrementSkipped();
                        continue;
                    }

                    int tableId = rs.getInt("AD_Table_ID");
                    String tableName = rs.getString("TableName");

                    Metadata metadata = Metadata.from("source_type", SOURCE_TYPE)
                        .put("source_id", String.valueOf(tableId))
                        .put("source_table", "AD_Table")
                        .put("entity_type", "table")
                        .put("title", tableName)
                        .put("ad_client_id", String.valueOf(clientId));

                    TextSegment segment = TextSegment.from(text, metadata);
                    Embedding embedding = embeddingModel.embed(segment).content();
                    store.add(embedding, segment);

                    result.incrementAdded();

                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to ingest table", e);
                    result.addError("Table: " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "Failed to query tables", e);
            result.addError("Tables query failed: " + e.getMessage());
        } finally {
            DB.close(rs, pstmt);
        }

        log.info("Ingested " + (result.getDocumentsAdded() - addedBefore) + " tables");
    }

    /**
     * Build text content for table embedding.
     */
    private String buildTableText(ResultSet rs) throws SQLException {
        StringBuilder sb = new StringBuilder();

        String tableName = rs.getString("TableName");
        String name = rs.getString("Name");
        String desc = rs.getString("Description");
        int columnCount = rs.getInt("ColumnCount");

        sb.append("Table: ").append(tableName).append("\n");

        if (name != null && !name.equals(tableName)) {
            sb.append("Display Name: ").append(name).append("\n");
        }

        if (desc != null && !desc.trim().isEmpty()) {
            sb.append("Description: ").append(desc).append("\n");
        }

        sb.append("Columns: ").append(columnCount).append("\n");

        return sb.toString();
    }

    /**
     * Update ingestion metadata table.
     */
    private void updateIngestionMetadata(Properties ctx, IngestResult result) {
        int clientId = Env.getAD_Client_ID(ctx);

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

        // Check if any AD entity was updated since last ingestion
        String sql = "SELECT MAX(Updated) as LastUpdate FROM (" +
                    "SELECT MAX(Updated) as Updated FROM AD_Window WHERE IsActive = 'Y' " +
                    "UNION ALL " +
                    "SELECT MAX(Updated) FROM AD_Process WHERE IsActive = 'Y' " +
                    "UNION ALL " +
                    "SELECT MAX(Updated) FROM AD_Table WHERE IsActive = 'Y'" +
                    ") updates";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, null);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                Timestamp lastUpdate = rs.getTimestamp("LastUpdate");
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

        String sql = "SELECT " +
                    "(SELECT COUNT(*) FROM AD_Window WHERE IsActive = 'Y' AND (AD_Client_ID = 0 OR AD_Client_ID = ?)) + " +
                    "(SELECT COUNT(*) FROM AD_Process WHERE IsActive = 'Y' AND IsReport = 'N' AND (AD_Client_ID = 0 OR AD_Client_ID = ?)) + " +
                    "(SELECT COUNT(*) FROM AD_Table WHERE IsActive = 'Y' AND IsView = 'N' AND (AD_Client_ID = 0 OR AD_Client_ID = ?)) " +
                    "as total";

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, null);
            pstmt.setInt(1, clientId);
            pstmt.setInt(2, clientId);
            pstmt.setInt(3, clientId);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
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
