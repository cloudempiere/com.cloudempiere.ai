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
package com.cloudempiere.ai.provider.langchain4j;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.compiere.model.MChat;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

import com.cloudempiere.ai.model.MAIChatEntry;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;

/**
 * Thread-aware ChatMemory implementation for iDempiere chat integration.
 *
 * <p>This implementation:
 * <ul>
 *   <li>Loads conversation history from CM_ChatEntry on initialization</li>
 *   <li>Persists new messages to CM_ChatEntry</li>
 *   <li>Filters messages by thread (CM_ChatEntryParent_ID)</li>
 *   <li>Supports thread switching for multi-thread conversations</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * ThreadAwareChatMemory memory = ThreadAwareChatMemory.builder()
 *     .chat(chat)
 *     .threadRootId(threadRootId)
 *     .maxMessages(20)
 *     .build();
 *
 * // Switch threads
 * memory.switchThread(newThreadRootId);
 *
 * // Create new thread
 * memory.createNewThread();
 * </pre>
 *
 * <p><b>Tags:</b> #chat #memory #langchain4j #thread-aware #persistence
 *
 * @author Cloudempiere
 * @version 0.14.0
 * @since ADR-031 Chat Panel LangChain4j Integration
 * @see ChatMemory
 * @see MAIChatEntry
 */
public class ThreadAwareChatMemory implements ChatMemory {

    private static final CLogger log = CLogger.getCLogger(ThreadAwareChatMemory.class);

    /** Parent chat */
    private final MChat chat;

    /** iDempiere context */
    private final Properties ctx;

    /** Transaction name */
    private final String trxName;

    /** Maximum messages to keep in memory */
    private final int maxMessages;

    /** Current thread root ID (0 = root level messages) */
    private int currentThreadRootId;

    /** Cached messages for current thread */
    private List<ChatMessage> messages;

    /** Flag to persist messages to database */
    private final boolean persistMessages;

    /** AI User ID for detecting AI responses */
    private Integer aiUserId;

    /**
     * Private constructor - use builder.
     */
    private ThreadAwareChatMemory(MChat chat, int threadRootId, int maxMessages,
                                   boolean persistMessages, Integer aiUserId) {
        this.chat = chat;
        this.ctx = chat.getCtx();
        this.trxName = chat.get_TrxName();
        this.currentThreadRootId = threadRootId;
        this.maxMessages = maxMessages;
        this.persistMessages = persistMessages;
        this.aiUserId = aiUserId;
        this.messages = new ArrayList<>();

        // Load existing messages from database
        loadMessages();
    }

    /**
     * Get the memory ID (used by LangChain4j for memory management).
     *
     * @return Memory ID combining chat ID and thread root ID
     */
    @Override
    public Object id() {
        return chat.getCM_Chat_ID() + "-" + currentThreadRootId;
    }

    /**
     * Add a message to memory.
     *
     * <p>If persistence is enabled, the message is also saved to CM_ChatEntry.
     *
     * @param message Chat message to add
     */
    @Override
    public void add(ChatMessage message) {
        messages.add(message);
        trimToMaxSize();

        if (persistMessages) {
            persistMessage(message);
        }

        log.fine("Added message to thread " + currentThreadRootId +
                 ", type=" + message.type() + ", total=" + messages.size());
    }

    /**
     * Get all messages in the current thread.
     *
     * @return List of chat messages (unmodifiable)
     */
    @Override
    public List<ChatMessage> messages() {
        return Collections.unmodifiableList(messages);
    }

    /**
     * Clear all messages from current thread memory.
     *
     * <p>Note: This only clears the in-memory cache, not the database.
     */
    @Override
    public void clear() {
        messages.clear();
        log.info("Cleared memory for thread " + currentThreadRootId);
    }

    /**
     * Switch to a different thread.
     *
     * @param newThreadRootId New thread root ID
     */
    public void switchThread(int newThreadRootId) {
        if (this.currentThreadRootId != newThreadRootId) {
            this.currentThreadRootId = newThreadRootId;
            this.messages = new ArrayList<>();
            loadMessages();
            log.info("Switched to thread " + newThreadRootId +
                     ", loaded " + messages.size() + " messages");
        }
    }

    /**
     * Create a new thread (clears current memory).
     *
     * <p>The new thread root ID will be set when the first message is persisted.
     */
    public void createNewThread() {
        this.currentThreadRootId = 0;
        this.messages = new ArrayList<>();
        log.info("Created new thread");
    }

    /**
     * Get current thread root ID.
     *
     * @return Thread root ID (0 = root level)
     */
    public int getCurrentThreadRootId() {
        return currentThreadRootId;
    }

    /**
     * Set current thread root ID.
     *
     * @param threadRootId Thread root ID
     */
    public void setCurrentThreadRootId(int threadRootId) {
        this.currentThreadRootId = threadRootId;
    }

    /**
     * Get message count.
     *
     * @return Number of messages in current thread
     */
    public int getMessageCount() {
        return messages.size();
    }

    /**
     * Load messages from CM_ChatEntry for current thread.
     */
    private void loadMessages() {
        messages.clear();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT CM_ChatEntry_ID, CharacterData, AD_User_ID, CM_ChatEntryParent_ID ");
        sql.append("FROM CM_ChatEntry ");
        sql.append("WHERE CM_Chat_ID = ? ");

        if (currentThreadRootId > 0) {
            // Load messages in this thread (parent + children)
            sql.append("AND (CM_ChatEntry_ID = ? OR CM_ChatEntryParent_ID = ?) ");
        } else {
            // Load root level messages only (no parent)
            sql.append("AND CM_ChatEntryParent_ID IS NULL ");
        }

        sql.append("ORDER BY Created ASC");

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql.toString(), trxName);
            pstmt.setInt(1, chat.getCM_Chat_ID());
            if (currentThreadRootId > 0) {
                pstmt.setInt(2, currentThreadRootId);
                pstmt.setInt(3, currentThreadRootId);
            }

            rs = pstmt.executeQuery();
            while (rs.next()) {
                String content = rs.getString("CharacterData");
                int userId = rs.getInt("AD_User_ID");

                ChatMessage message = convertToChatMessage(content, userId);
                if (message != null) {
                    messages.add(message);
                }
            }

            // Trim to max size
            trimToMaxSize();

            log.fine("Loaded " + messages.size() + " messages for chat " +
                     chat.getCM_Chat_ID() + ", thread " + currentThreadRootId);

        } catch (Exception e) {
            log.warning("Failed to load messages: " + e.getMessage());
        } finally {
            DB.close(rs, pstmt);
        }
    }

    /**
     * Convert database entry to LangChain4j ChatMessage.
     *
     * @param content Message content
     * @param userId AD_User_ID of message creator
     * @return ChatMessage or null
     */
    private ChatMessage convertToChatMessage(String content, int userId) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        // Determine if this is an AI response
        if (isAIUser(userId)) {
            return AiMessage.from(content);
        } else {
            return UserMessage.from(content);
        }
    }

    /**
     * Check if the user ID represents the AI user.
     *
     * @param userId User ID to check
     * @return true if AI user
     */
    private boolean isAIUser(int userId) {
        if (aiUserId != null) {
            return aiUserId.equals(userId);
        }
        // Fallback: System user (0) is considered AI
        return userId == 0;
    }

    /**
     * Persist a message to CM_ChatEntry.
     *
     * @param message Message to persist
     */
    private void persistMessage(ChatMessage message) {
        try {
            MAIChatEntry entry;
            String content = getMessageContent(message);

            if (message instanceof AiMessage) {
                // ADR-054: Render AI message markdown to HTML before persisting
                java.util.Locale userLocale = org.compiere.util.Env.getLanguage(ctx).getLocale();
                String contentHtml = com.cloudempiere.ai.util.AIMessageRenderer.render(
                    content, ctx, null, userLocale);
                entry = MAIChatEntry.createAIResponse(chat, contentHtml);
            } else {
                entry = new MAIChatEntry(chat, content);
            }

            // Set thread parent if in a thread
            if (currentThreadRootId > 0) {
                entry.setCM_ChatEntryParent_ID(currentThreadRootId);
            }

            entry.saveEx();

            // Update thread root ID if this is the first message in a new thread
            if (currentThreadRootId == 0 && message instanceof UserMessage) {
                currentThreadRootId = entry.getCM_ChatEntry_ID();
            }

            log.fine("Persisted message " + entry.getCM_ChatEntry_ID() +
                     " to thread " + currentThreadRootId);

        } catch (Exception e) {
            log.warning("Failed to persist message: " + e.getMessage());
        }
    }

    /**
     * Get text content from a ChatMessage.
     *
     * @param message Chat message
     * @return Text content
     */
    private String getMessageContent(ChatMessage message) {
        if (message instanceof UserMessage) {
            return ((UserMessage) message).singleText();
        } else if (message instanceof AiMessage) {
            return ((AiMessage) message).text();
        } else if (message instanceof SystemMessage) {
            return ((SystemMessage) message).text();
        }
        return message.toString();
    }

    /**
     * Trim messages to max size (keep most recent).
     */
    private void trimToMaxSize() {
        if (messages.size() > maxMessages) {
            int excess = messages.size() - maxMessages;
            messages = new ArrayList<>(messages.subList(excess, messages.size()));
            log.fine("Trimmed " + excess + " old messages");
        }
    }

    // ========================================================================
    // Builder
    // ========================================================================

    /**
     * Create a new builder.
     *
     * @return Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for ThreadAwareChatMemory.
     */
    public static class Builder {
        private MChat chat;
        private int threadRootId = 0;
        private int maxMessages = 20;
        private boolean persistMessages = true;
        private Integer aiUserId;

        /**
         * Set the parent chat.
         *
         * @param chat Parent chat (required)
         * @return this builder
         */
        public Builder chat(MChat chat) {
            this.chat = chat;
            return this;
        }

        /**
         * Set the thread root ID.
         *
         * @param threadRootId Thread root ID (0 = root level)
         * @return this builder
         */
        public Builder threadRootId(int threadRootId) {
            this.threadRootId = threadRootId;
            return this;
        }

        /**
         * Set the maximum number of messages to keep.
         *
         * @param maxMessages Maximum messages (default: 20)
         * @return this builder
         */
        public Builder maxMessages(int maxMessages) {
            this.maxMessages = maxMessages;
            return this;
        }

        /**
         * Set whether to persist messages to database.
         *
         * @param persistMessages true to persist (default: true)
         * @return this builder
         */
        public Builder persistMessages(boolean persistMessages) {
            this.persistMessages = persistMessages;
            return this;
        }

        /**
         * Set the AI user ID for message role detection.
         *
         * @param aiUserId AI User ID
         * @return this builder
         */
        public Builder aiUserId(Integer aiUserId) {
            this.aiUserId = aiUserId;
            return this;
        }

        /**
         * Build the ThreadAwareChatMemory instance.
         *
         * @return ThreadAwareChatMemory instance
         * @throws IllegalStateException if chat is not set
         */
        public ThreadAwareChatMemory build() {
            if (chat == null) {
                throw new IllegalStateException("Chat is required");
            }
            return new ThreadAwareChatMemory(chat, threadRootId, maxMessages,
                                              persistMessages, aiUserId);
        }
    }
}
