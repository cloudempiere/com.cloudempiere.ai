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
package com.cloudempiere.ai.event;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventManager;
import org.adempiere.base.event.IEventTopics;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;

import com.cloudempiere.ai.rag.embedding.IEmbeddingTriggerService;
import com.cloudempiere.ai.rag.embedding.IEmbeddingTriggerService.EmbeddingResult;
import com.cloudempiere.ai.util.EditorJsParser;

/**
 * Event Handler for K_Entry table changes - triggers embedding generation.
 *
 * <p>This handler implements the "Push to Vector on K_Entry Change" pattern
 * from ADR-029. When a K_Entry record is created or modified:
 * <ol>
 *   <li>Parse EditorJS content (TextMsg field)</li>
 *   <li>Generate embedding via LangChain4j</li>
 *   <li>Store in pgvector with metadata</li>
 * </ol>
 *
 * <p>This allows knowledge base content to be immediately searchable via RAG
 * without requiring batch re-indexing.
 *
 * <p><b>Source fields used:</b>
 * <ul>
 *   <li>Name - Entry title</li>
 *   <li>TextMsg - EditorJS JSON content</li>
 *   <li>Keywords - Additional searchable terms</li>
 *   <li>K_Topic - Topic context</li>
 *   <li>K_Type - Entry type</li>
 * </ul>
 *
 * @author Cloudempiere AI Team
 * @version ADR-029
 * @since v0.12.0
 * @see IEmbeddingTriggerService
 * @see EditorJsParser
 */
@Component(
    reference = @Reference(
        name = "IEventManager",
        bind = "bindEventManager",
        unbind = "unbindEventManager",
        policy = ReferencePolicy.STATIC,
        cardinality = ReferenceCardinality.MANDATORY,
        service = IEventManager.class
    )
)
public class KEntryEventHandler extends AbstractEventHandler {

    private static final CLogger log = CLogger.getCLogger(KEntryEventHandler.class);

    /** K_Entry table name */
    private static final String TABLE_NAME = "K_Entry";

    /** Source type for embedding metadata */
    private static final String SOURCE_TYPE = "knowledge_entry";

    /** Embedding trigger service - injected via OSGi DS */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IEmbeddingTriggerService embeddingService;

    @Override
    protected void initialize() {
        // Register for K_Entry table events
        registerTableEvent(IEventTopics.PO_AFTER_NEW, TABLE_NAME);
        registerTableEvent(IEventTopics.PO_AFTER_CHANGE, TABLE_NAME);
        registerTableEvent(IEventTopics.PO_AFTER_DELETE, TABLE_NAME);
        registerTableEvent(IEventTopics.PO_AFTER_NEW_REPLICATION, TABLE_NAME);
        registerTableEvent(IEventTopics.PO_AFTER_CHANGE_REPLICATION, TABLE_NAME);

        log.info("KEntryEventHandler initialized - listening for K_Entry changes to trigger embedding");
    }

    @Override
    protected void doHandleEvent(Event event) {
        PO po = getPO(event);
        if (po == null) {
            return;
        }

        String topic = event.getTopic();

        // Handle delete events
        if (IEventTopics.PO_AFTER_DELETE.equals(topic)) {
            handleDelete(po);
            return;
        }

        // Handle create/update events
        if (IEventTopics.PO_AFTER_NEW.equals(topic) ||
            IEventTopics.PO_AFTER_CHANGE.equals(topic) ||
            IEventTopics.PO_AFTER_NEW_REPLICATION.equals(topic) ||
            IEventTopics.PO_AFTER_CHANGE_REPLICATION.equals(topic)) {
            handleCreateOrUpdate(po, topic);
        }
    }

    /**
     * Handle K_Entry create or update - generate embedding.
     */
    private void handleCreateOrUpdate(PO po, String topic) {
        // Check if embedding service is available
        IEmbeddingTriggerService service = embeddingService;
        if (service == null || !service.isAvailable()) {
            log.fine("Embedding service not available - skipping K_Entry embedding");
            return;
        }

        // Skip inactive entries
        if (!po.isActive()) {
            log.fine("K_Entry is inactive - skipping embedding");
            return;
        }

        try {
            Properties ctx = po.getCtx();
            int entryId = po.get_ID();
            String sourceId = String.valueOf(entryId);

            // Extract content from K_Entry
            String name = (String) po.get_Value("Name");
            String textMsg = (String) po.get_Value("TextMsg");
            String keywords = (String) po.get_Value("Keywords");

            // Build embeddable text content
            String textContent = buildTextContent(po, name, textMsg, keywords);

            if (textContent == null || textContent.trim().isEmpty()) {
                log.fine("K_Entry has no embeddable content - skipping");
                // Delete any existing embedding for empty content
                service.deleteEmbedding(ctx, SOURCE_TYPE, sourceId);
                return;
            }

            // Add or update embedding
            EmbeddingResult result = service.addOrUpdateEmbedding(
                ctx,
                SOURCE_TYPE,
                sourceId,
                TABLE_NAME,
                textContent,
                name,
                null  // Language could be extracted from context
            );

            if (result.isSuccess()) {
                log.info("K_Entry embedding " + result.getStatus() + " for ID " + entryId +
                        " [" + result.getDurationMs() + "ms]");
            } else if (!result.isSkipped()) {
                log.warning("K_Entry embedding failed for ID " + entryId + ": " + result.getErrorMessage());
            }

        } catch (Exception e) {
            log.log(Level.SEVERE, "Error handling K_Entry embedding for ID " + po.get_ID(), e);
        }
    }

    /**
     * Handle K_Entry delete - remove embedding.
     */
    private void handleDelete(PO po) {
        IEmbeddingTriggerService service = embeddingService;
        if (service == null || !service.isAvailable()) {
            return;
        }

        try {
            Properties ctx = po.getCtx();
            String sourceId = String.valueOf(po.get_ID());

            int deleted = service.deleteEmbedding(ctx, SOURCE_TYPE, sourceId);

            if (deleted > 0) {
                log.info("Deleted " + deleted + " embedding(s) for K_Entry ID " + po.get_ID());
            }

        } catch (Exception e) {
            log.log(Level.WARNING, "Error deleting K_Entry embedding for ID " + po.get_ID(), e);
        }
    }

    /**
     * Build text content for embedding from K_Entry fields.
     *
     * <p>Combines:
     * <ul>
     *   <li>Entry name (title)</li>
     *   <li>Topic name (if available)</li>
     *   <li>Type name (if available)</li>
     *   <li>Keywords</li>
     *   <li>TextMsg content (parsed from EditorJS if JSON)</li>
     * </ul>
     */
    private String buildTextContent(PO po, String name, String textMsg, String keywords) {
        StringBuilder sb = new StringBuilder();

        // Add title
        if (name != null && !name.isEmpty()) {
            sb.append("Title: ").append(name).append("\n");
        }

        // Add topic name (lookup)
        String topicName = getTopicName(po);
        if (topicName != null && !topicName.isEmpty()) {
            sb.append("Topic: ").append(topicName).append("\n");
        }

        // Add type name (lookup)
        String typeName = getTypeName(po);
        if (typeName != null && !typeName.isEmpty()) {
            sb.append("Type: ").append(typeName).append("\n");
        }

        // Add keywords
        if (keywords != null && !keywords.isEmpty()) {
            sb.append("Keywords: ").append(keywords).append("\n");
        }

        // Add content - parse EditorJS if applicable
        if (textMsg != null && !textMsg.isEmpty()) {
            String content = parseTextMsg(textMsg);
            if (content != null && !content.isEmpty()) {
                sb.append("\n").append(content);
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
                // Parse EditorJS to markdown/text
                String markdown = EditorJsParser.parseToMarkdown(textMsg);
                if (markdown != null && !markdown.isEmpty()) {
                    return markdown;
                }
            } catch (Exception e) {
                log.log(Level.FINE, "EditorJS parsing failed, using raw text", e);
            }
        }

        // Return as-is if not EditorJS or parsing failed
        return textMsg;
    }

    /**
     * Get K_Topic name for this entry.
     */
    private String getTopicName(PO po) {
        Object topicIdObj = po.get_Value("K_Topic_ID");
        if (topicIdObj == null) {
            return null;
        }

        int topicId = ((Number) topicIdObj).intValue();
        if (topicId <= 0) {
            return null;
        }

        String sql = "SELECT Name FROM K_Topic WHERE K_Topic_ID = ?";
        try (Connection conn = DB.getConnectionRO();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, topicId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Name");
                }
            }
        } catch (SQLException e) {
            log.log(Level.FINE, "Failed to get topic name", e);
        }
        return null;
    }

    /**
     * Get K_Type name for this entry.
     */
    private String getTypeName(PO po) {
        Object typeIdObj = po.get_Value("K_Type_ID");
        if (typeIdObj == null) {
            return null;
        }

        int typeId = ((Number) typeIdObj).intValue();
        if (typeId <= 0) {
            return null;
        }

        String sql = "SELECT Name FROM K_Type WHERE K_Type_ID = ?";
        try (Connection conn = DB.getConnectionRO();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, typeId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Name");
                }
            }
        } catch (SQLException e) {
            log.log(Level.FINE, "Failed to get type name", e);
        }
        return null;
    }
}
