package com.cloudempiere.ai.kb.agent;

import java.util.Map;
import java.util.logging.Logger;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.cloudempiere.ai.boundary.IDomainAgent;

import com.cloudempiere.ai.kb.tools.KbTools;
import com.cloudempiere.ai.provider.langchain4j.ILangChain4jProviderFactory;
import com.cloudempiere.ai.model.MAIProvider;

import org.compiere.util.Env;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

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
 * @author Cloudempiere AI Team
 * @version 1.0.0
 */
@Component(service = {KbAgent.class, IDomainAgent.class}, immediate = true)
public class KbAgent implements IDomainAgent {

    private static final Logger log = Logger.getLogger(KbAgent.class.getName());

    // CLD-1955: OPTIONAL+DYNAMIC so the agent can activate even when the LangChain4j provider bundle is absent
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    private volatile ILangChain4jProviderFactory providerFactory;

    @Reference
    private volatile KbTools kbTools;

    private KbAgentInterface agent;

    /**
     * Activate the knowledge base agent.
     *
     * <p>This method is called by OSGi when the component is activated.
     * The actual agent initialization is deferred until first use.</p>
     */
    @Activate
    protected void activate() {
        long startTime = System.currentTimeMillis();
        log.warning("[STARTUP TIMING] KbAgent.activate() START");

        log.info("Knowledge Base Agent OSGi component activated and ready");

        long elapsed = System.currentTimeMillis() - startTime;
        log.warning("[STARTUP TIMING] KbAgent.activate() COMPLETED in " + elapsed + "ms");
    }

    private synchronized void ensureInitialized() {
        if (agent != null) {
            return;
        }
        if (providerFactory == null) {
            throw new IllegalStateException("AI provider factory not available — langchain4j provider bundle not installed");
        }

        try {
            MAIProvider providerConfig = MAIProvider.getDefault(Env.getCtx(), null);

            if (providerConfig == null) {
                throw new RuntimeException("No AI provider configured. Please create an AIG_Provider record with IsDefault='Y'");
            }

            log.info("Initializing Knowledge Base Agent with provider: " + providerConfig.getName());

            ChatLanguageModel model = providerFactory.createModel(providerConfig);

            agent = AiServices.builder(KbAgentInterface.class)
                .chatLanguageModel(model)
                .tools(kbTools)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .build();

            log.info("Knowledge Base Agent initialized successfully");

        } catch (Exception e) {
            log.severe("Failed to initialize Knowledge Base Agent: " + e.getMessage());
            throw new RuntimeException("Knowledge Base Agent initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Search for relevant articles.
     *
     * @param query search query
     * @return search results with recommendations
     */
    public String searchKnowledgeBase(String query) {
        ensureInitialized();
        return agent.searchKnowledgeBase(query);
    }

    /**
     * Get article summary and analysis.
     *
     * @param entryId the K_Entry_ID
     * @return article summary and related content
     */
    public String summarizeArticle(int entryId) {
        ensureInitialized();
        return agent.summarizeArticle(entryId);
    }

    /**
     * Get knowledge base insights from natural language query.
     *
     * @param query natural language query
     * @return AI-generated response with insights
     */
    public String chat(String query) {
        ensureInitialized();
        return agent.chat(query);
    }

    @Override
    public String getDomain() {
        return "kb";
    }

    @Override
    public boolean canHandle(String query, Map<String, Object> context) {
        if (query == null) return false;
        String lower = query.toLowerCase();
        String[] keywords = {"documentation", "article", "guide", "how to", "tutorial", "manual", "knowledge", "wiki", "faq", "help"};
        for (String kw : keywords) {
            if (lower.contains(kw)) return true;
        }
        if (context != null && context.containsKey("tableName")) {
            String tn = ((String) context.get("tableName")).toLowerCase();
            if (tn.contains("k_entry") || tn.contains("knowledge") || tn.contains("k_category")) return true;
        }
        return false;
    }

    @Override
    public String process(String query, Map<String, Object> context) {
        return chat(query);
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
        @SystemMessage("You are a knowledge base specialist AI for iDempiere ERP documentation and support.\n\n" +
            "Your role is to help users find and understand knowledge base content:\n" +
            "- Searching articles by keywords, topics, and context\n" +
            "- Summarizing article content and key points\n" +
            "- Recommending related articles and resources\n" +
            "- Identifying knowledge gaps and suggesting improvements\n" +
            "- Connecting support tickets to relevant documentation\n\n" +
            "You have access to the following through tools:\n" +
            "- Knowledge base articles (titles, summaries, full content)\n" +
            "- Article categories and classifications\n" +
            "- Article ratings and user comments\n" +
            "- Related articles and cross-references\n" +
            "- Top-rated and most helpful articles\n\n" +
            "When helping users:\n" +
            "1. Always search for relevant articles before providing information\n" +
            "2. Summarize content clearly and concisely\n" +
            "3. Provide article IDs and titles for reference\n" +
            "4. Suggest related articles for deeper learning\n" +
            "5. Identify when information is missing or outdated\n" +
            "6. Consider article ratings and user feedback\n\n" +
            "Search strategy:\n" +
            "- Use multiple search terms if initial search yields no results\n" +
            "- Search by category when topic is clear\n" +
            "- Recommend top-rated articles for common questions\n" +
            "- Combine information from multiple articles when needed\n\n" +
            "Security boundaries:\n" +
            "- You can READ all published knowledge base articles\n" +
            "- You can CREATE draft articles (requires human review before publishing)\n" +
            "- You can ADD comments to articles\n" +
            "- You CANNOT modify categories or configuration\n" +
            "- You CANNOT publish articles (requires human approval)\n" +
            "- You CANNOT access restricted or unpublished content\n\n" +
            "Always cite sources with article IDs and provide clear navigation to relevant content.")
        String chat(@UserMessage String query);

        /**
         * Search knowledge base for relevant content.
         *
         * @param query the search query
         * @return search results with recommendations
         */
        @UserMessage("Search the knowledge base for: {{query}}\n\n" +
            "Provide:\n" +
            "1. Most relevant articles (with IDs and summaries)\n" +
            "2. Article ratings and reliability\n" +
            "3. Related categories to explore\n" +
            "4. Additional search suggestions if results are limited\n" +
            "5. Recommended reading order if multiple articles apply\n\n" +
            "If no exact matches found, suggest alternative search terms or related topics.")
        String searchKnowledgeBase(String query);

        /**
         * Summarize a knowledge base article.
         *
         * @param entryId the article to summarize
         * @return article summary and analysis
         */
        @UserMessage("Summarize knowledge base article {{entryId}}. Provide:\n" +
            "1. Article title and metadata (category, author, rating)\n" +
            "2. Key points summary (3-5 bullet points)\n" +
            "3. Full content overview\n" +
            "4. Related articles and cross-references\n" +
            "5. User comments and feedback highlights\n" +
            "6. Recommendations for further reading\n\n" +
            "Make the summary concise but comprehensive enough to understand the article's value.")
        String summarizeArticle(int entryId);
    }
}
