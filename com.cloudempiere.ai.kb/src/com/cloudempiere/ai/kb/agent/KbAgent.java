package com.cloudempiere.ai.kb.agent;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.cloudempiere.ai.provider.IAIProvider;
import com.cloudempiere.ai.provider.factory.IAIProviderFactory;
import com.cloudempiere.ai.kb.tools.KbTools;

import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * Knowledge Base domain AI agent.
 *
 * <p>This agent provides AI-powered knowledge base assistance including:</p>
 * <ul>
 *   <li>Article search and retrieval</li>
 *   <li>Content recommendations</li>
 *   <li>Related article suggestions</li>
 *   <li>Knowledge gap identification</li>
 *   <li>Article summarization</li>
 * </ul>
 *
 * <p><b>Architecture Layer:</b> Domain Agent Layer (Knowledge Base)</p>
 * <p><b>Boundary:</b> {@link com.cloudempiere.ai.kb.boundary.KbDomainBoundary}</p>
 * <p><b>Tools:</b> {@link com.cloudempiere.ai.kb.tools.KbTools}</p>
 *
 * <p><b>Related ADRs:</b></p>
 * <ul>
 *   <li><a href="../../../docs/adr/009-domain-boundaries-agent-scope.md">ADR-009: Domain Boundaries</a></li>
 *   <li><a href="../../../docs/adr/011-specialized-agent-scopes.md">ADR-011: Specialized Agent Scopes</a></li>
 *   <li><a href="../../../docs/adr/016-knowledge-base-agent.md">ADR-016: Knowledge Base Agent Domain</a></li>
 * </ul>
 *
 * @author CloudEmpiere AI Team
 * @version 1.0.0
 */
@Component(service = KbAgent.class, immediate = true)
public class KbAgent {

    private static final Logger log = Logger.getLogger(KbAgent.class.getName());

    @Reference
    private volatile IAIProviderFactory providerFactory;

    @Reference
    private volatile KbTools kbTools;

    private KbAgentInterface agent;

    /**
     * Activate the knowledge base agent.
     *
     * <p>This method is called by OSGi when the component is activated.
     * It initializes the LangChain4j agent with the configured AI provider
     * and KB tools.</p>
     */
    @Activate
    protected void activate() {
        log.info("Activating Knowledge Base Agent...");

        try {
            // Get default AI provider (TODO: make configurable per client)
            IAIProvider provider = providerFactory.getDefaultProvider();

            if (provider == null) {
                log.warning("No AI provider configured. Knowledge Base agent will not be available.");
                return;
            }

            // Build agent with LangChain4j
            agent = AiServices.builder(KbAgentInterface.class)
                .chatLanguageModel(provider.getChatModel())
                .tools(kbTools)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .build();

            log.info("Knowledge Base Agent activated successfully with provider: " + provider.getProviderName());

        } catch (Exception e) {
            log.severe("Failed to activate Knowledge Base Agent: " + e.getMessage());
            throw new RuntimeException("Knowledge Base Agent activation failed", e);
        }
    }

    /**
     * Search for relevant articles.
     *
     * @param query search query
     * @return search results with recommendations
     */
    public String searchKnowledgeBase(String query) {
        if (agent == null) {
            return "Knowledge Base agent not available. Please configure an AI provider.";
        }
        return agent.searchKnowledgeBase(query);
    }

    /**
     * Get article summary and analysis.
     *
     * @param entryId the K_Entry_ID
     * @return article summary and related content
     */
    public String summarizeArticle(int entryId) {
        if (agent == null) {
            return "Knowledge Base agent not available. Please configure an AI provider.";
        }
        return agent.summarizeArticle(entryId);
    }

    /**
     * Get knowledge base insights from natural language query.
     *
     * @param query natural language query
     * @return AI-generated response with insights
     */
    public String chat(String query) {
        if (agent == null) {
            return "Knowledge Base agent not available. Please configure an AI provider.";
        }
        return agent.chat(query);
    }

    /**
     * Knowledge Base agent interface for LangChain4j.
     *
     * <p>This interface defines the AI agent's capabilities using
     * LangChain4j's declarative service annotations.</p>
     */
    interface KbAgentInterface {

        /**
         * System message defining the agent's role and capabilities.
         */
        @SystemMessage("""
            You are a knowledge base specialist AI for iDempiere ERP documentation and support.

            Your role is to help users find and understand knowledge base content:
            - Searching articles by keywords, topics, and context
            - Summarizing article content and key points
            - Recommending related articles and resources
            - Identifying knowledge gaps and suggesting improvements
            - Connecting support tickets to relevant documentation

            You have access to the following through tools:
            - Knowledge base articles (titles, summaries, full content)
            - Article categories and classifications
            - Article ratings and user comments
            - Related articles and cross-references
            - Top-rated and most helpful articles

            When helping users:
            1. Always search for relevant articles before providing information
            2. Summarize content clearly and concisely
            3. Provide article IDs and titles for reference
            4. Suggest related articles for deeper learning
            5. Identify when information is missing or outdated
            6. Consider article ratings and user feedback

            Search strategy:
            - Use multiple search terms if initial search yields no results
            - Search by category when topic is clear
            - Recommend top-rated articles for common questions
            - Combine information from multiple articles when needed

            Security boundaries:
            - You can READ all published knowledge base articles
            - You can CREATE draft articles (requires human review before publishing)
            - You can ADD comments to articles
            - You CANNOT modify categories or configuration
            - You CANNOT publish articles (requires human approval)
            - You CANNOT access restricted or unpublished content

            Always cite sources with article IDs and provide clear navigation to relevant content.
            """)
        String chat(@UserMessage String query);

        /**
         * Search knowledge base for relevant content.
         *
         * @param query the search query
         * @return search results with recommendations
         */
        @UserMessage("""
            Search the knowledge base for: {{query}}

            Provide:
            1. Most relevant articles (with IDs and summaries)
            2. Article ratings and reliability
            3. Related categories to explore
            4. Additional search suggestions if results are limited
            5. Recommended reading order if multiple articles apply

            If no exact matches found, suggest alternative search terms or related topics.
            """)
        String searchKnowledgeBase(String query);

        /**
         * Summarize a knowledge base article.
         *
         * @param entryId the article to summarize
         * @return article summary and analysis
         */
        @UserMessage("""
            Summarize knowledge base article {{entryId}}. Provide:
            1. Article title and metadata (category, author, rating)
            2. Key points summary (3-5 bullet points)
            3. Full content overview
            4. Related articles and cross-references
            5. User comments and feedback highlights
            6. Recommendations for further reading

            Make the summary concise but comprehensive enough to understand the article's value.
            """)
        String summarizeArticle(int entryId);
    }
}
