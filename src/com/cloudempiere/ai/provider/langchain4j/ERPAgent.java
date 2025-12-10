package com.cloudempiere.ai.provider.langchain4j;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * ERP AI Agent interface for LangChain4j AiServices.
 *
 * This interface defines the contract for AI agents that interact with
 * the ERP system. LangChain4j AiServices creates a dynamic proxy that:
 * - Routes user messages to the AI model
 * - Automatically handles tool/function calling
 * - Manages conversation memory per session
 *
 * Usage:
 * <pre>
 * ChatLanguageModel model = LangChain4jProviderFactory.create(config);
 * ERPTools tools = new ERPTools(provider, ctx);
 *
 * ERPAgent agent = AiServices.builder(ERPAgent.class)
 *     .chatLanguageModel(model)
 *     .tools(tools)
 *     .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
 *     .build();
 *
 * String response = agent.chat(sessionId, "Show me overdue orders");
 * </pre>
 *
 * @author Cloudempiere
 * @version 0.13.0
 * @since ADR-002 LangChain4j Strategic Adoption
 */
public interface ERPAgent {

    /**
     * System message that defines the agent's behavior and constraints.
     */
    String SYSTEM_PROMPT =
        "You are an intelligent assistant for the iDempiere ERP system.\n\n" +
        "CRITICAL RULE - ALWAYS USE TOOLS FOR DATA:\n" +
        "When the user asks about data (customers, orders, products, invoices, etc.), " +
        "you MUST use the provided tools to query the database. NEVER make up or hallucinate data. " +
        "If you don't have access to data or a query fails, say so clearly.\n\n" +
        "CAPABILITIES:\n" +
        "- Query the ERP database to answer questions about orders, products, customers, inventory\n" +
        "- Look up specific records by ID or search value\n" +
        "- Search and filter records based on criteria\n" +
        "- Explain ERP data and business processes\n\n" +
        "SECURITY RULES:\n" +
        "- All queries are filtered by user's role permissions\n" +
        "- You cannot access data the user doesn't have permission to see\n" +
        "- Never expose sensitive data like passwords, API keys, or credit card numbers\n" +
        "- Always respect data confidentiality\n\n" +
        "BEHAVIOR:\n" +
        "- ALWAYS use tools to fetch real data - do not guess or make up data\n" +
        "- Be concise and accurate\n" +
        "- When querying data, explain what you found\n" +
        "- If a query returns no results, suggest alternative approaches\n" +
        "- Format numbers and dates in a readable way\n" +
        "- If you're unsure about something, say so\n\n" +
        "AVAILABLE TOOLS (USE THESE FOR DATA QUERIES):\n" +
        "- queryDatabase: Execute SQL SELECT queries against the ERP database\n" +
        "- lookupRecord: Get a specific record by ID from any table\n" +
        "- searchRecords: Search records with WHERE clause\n" +
        "- getTableMetadata: Get table structure information\n" +
        "- listTables: List available tables in the database\n" +
        "- getBusinessPartner: Look up customer/vendor details by ID or search key\n" +
        "- getProduct: Look up product details by ID or search key\n" +
        "- getOrder: Look up order details by DocumentNo or ID";

    /**
     * Main chat method for conversational interaction.
     *
     * <p>Note: System message is provided dynamically via systemMessageProvider()
     * in AiServices builder to support language detection (ADR-037).
     * Do NOT add @SystemMessage annotation here - it would override the dynamic prompt.
     *
     * @param sessionId Unique session identifier for conversation memory
     * @param userMessage The user's message or question
     * @return AI response with any relevant data
     */
    String chat(@MemoryId String sessionId, @UserMessage String userMessage);

    /**
     * Execute a specific task/goal without conversation context.
     *
     * <p>Note: System message is provided dynamically via systemMessageProvider().
     */
    String execute(@UserMessage String goal);
}
