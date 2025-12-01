package com.cloudempiere.ai.provider.langchain4j;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * iDempiere AI Agent interface for LangChain4j AiServices.
 *
 * This interface defines the contract for AI agents that interact with
 * iDempiere ERP. LangChain4j AiServices creates a dynamic proxy that:
 * - Routes user messages to the AI model
 * - Automatically handles tool/function calling
 * - Manages conversation memory per session
 *
 * Usage:
 * <pre>
 * ChatLanguageModel model = LangChain4jProviderFactory.create(config);
 * ERPTools tools = new ERPTools(provider, ctx);
 *
 * IDempiereAgent agent = AiServices.builder(IDempiereAgent.class)
 *     .chatLanguageModel(model)
 *     .tools(tools)
 *     .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
 *     .build();
 *
 * String response = agent.chat(sessionId, "Show me overdue orders");
 * </pre>
 *
 * @author CloudEmpiere
 * @version 0.9.0
 * @since ADR-002 LangChain4j Strategic Adoption
 */
public interface IDempiereAgent {

    /**
     * System message that defines the agent's behavior and constraints.
     */
    String SYSTEM_PROMPT = """
        You are an intelligent assistant for iDempiere ERP system.

        CAPABILITIES:
        - Query the ERP database to answer questions about orders, products, customers, inventory
        - Look up specific records by ID or search value
        - Search and filter records based on criteria
        - Explain ERP data and business processes

        SECURITY RULES:
        - All queries are filtered by user's role permissions
        - You cannot access data the user doesn't have permission to see
        - Never expose sensitive data like passwords, API keys, or credit card numbers
        - Always respect data confidentiality

        BEHAVIOR:
        - Be concise and accurate
        - When querying data, explain what you found
        - If a query returns no results, suggest alternative approaches
        - Format numbers and dates in a readable way
        - If you're unsure about something, say so

        AVAILABLE TOOLS:
        - queryDatabase: Execute SQL SELECT queries
        - lookupRecord: Get a specific record by ID
        - searchRecords: Search records with WHERE clause
        - getTableMetadata: Get table structure information
        - listTables: List available tables
        - getBusinessPartner: Look up customer/vendor details
        - getProduct: Look up product details
        - getOrder: Look up order details
        """;

    /**
     * Main chat method for conversational interaction.
     *
     * @param sessionId Unique session identifier for conversation memory
     * @param userMessage The user's message or question
     * @return AI response with any relevant data
     */
    @SystemMessage(SYSTEM_PROMPT)
    String chat(@MemoryId String sessionId, @UserMessage String userMessage);

    /**
     * Execute a specific task/goal without conversation context.
     */
    @SystemMessage(SYSTEM_PROMPT)
    String execute(@UserMessage String goal);
}
