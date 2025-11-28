/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                      *
 * Copyright (C) Cloudempiere, Inc. All Rights Reserved.                     *
 * This program is free software; you can redistribute it and/or modify it   *
 * under the terms version 2 of the GNU General Public License as published  *
 * by the Free Software Foundation. This program is distributed in the hope  *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied*
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.          *
 * See the GNU General Public License for more details.                      *
 * You should have received a copy of the GNU General Public License along   *
 * with this program; if not, write to the Free Software Foundation, Inc.,   *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                    *
 *****************************************************************************/
package com.cloudempiere.ai.agent.langchain4j;

import java.util.Properties;

import org.compiere.util.CLogger;

import com.cloudempiere.ai.agent.AgentContext;
import com.cloudempiere.ai.model.MAIGProvider;
import com.cloudempiere.ai.provider.IAIProvider;
import com.cloudempiere.ai.provider.factory.AIProviderFactory;

import dev.langchain4j.model.chat.ChatLanguageModel;

/**
 * Factory for creating LangChain4j-based agents
 *
 * <p>This factory creates {@link LangChain4jAgent} instances configured
 * with the appropriate ChatLanguageModel based on the iDempiere AI provider
 * configuration.
 *
 * <p>It bridges the gap between iDempiere's provider configuration
 * (stored in AIG_Provider table) and LangChain4j's ChatLanguageModel interface.
 *
 * <p>Usage:
 * <pre>
 * // From AgentContext
 * LangChain4jAgent agent = LangChain4jAgentFactory.createFromContext(context);
 *
 * // From provider ID
 * LangChain4jAgent agent = LangChain4jAgentFactory.createFromProvider(ctx, providerId);
 *
 * // Direct with ChatLanguageModel
 * ChatLanguageModel model = AnthropicChatModel.builder()...build();
 * LangChain4jAgent agent = LangChain4jAgentFactory.create("my-agent", model);
 * </pre>
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class LangChain4jAgentFactory {

    private static final CLogger log = CLogger.getCLogger(LangChain4jAgentFactory.class);

    /**
     * Create an agent directly from a ChatLanguageModel
     *
     * <p>Use this when you already have a configured ChatLanguageModel instance.
     *
     * @param name agent name
     * @param chatModel LangChain4j chat model
     * @return configured agent
     */
    public static LangChain4jAgent create(String name, ChatLanguageModel chatModel) {
        if (chatModel == null) {
            throw new IllegalArgumentException("ChatLanguageModel cannot be null");
        }

        log.info("Creating LangChain4j agent: " + name);
        return new LangChain4jAgent(name, chatModel);
    }

    /**
     * Create an agent from AgentContext
     *
     * <p>This loads the provider configuration from the database and creates
     * the appropriate ChatLanguageModel based on the provider type.
     *
     * @param context agent context with provider ID
     * @return configured agent
     * @throws IllegalStateException if provider not found or not supported
     */
    public static LangChain4jAgent createFromContext(AgentContext context) {
        if (context == null) {
            throw new IllegalArgumentException("AgentContext cannot be null");
        }

        return createFromProvider(context.getCtx(), context.getProviderId());
    }

    /**
     * Create an agent from provider ID
     *
     * @param ctx iDempiere context
     * @param providerId AIG_Provider_ID
     * @return configured agent
     * @throws IllegalStateException if provider not found or not supported
     */
    public static LangChain4jAgent createFromProvider(Properties ctx, int providerId) {
        if (providerId <= 0) {
            throw new IllegalArgumentException("Invalid provider ID: " + providerId);
        }

        // Load provider configuration
        MAIGProvider provider = MAIGProvider.get(ctx, providerId);
        if (provider == null) {
            throw new IllegalStateException("Provider not found: " + providerId);
        }

        log.info("Creating LangChain4j agent from provider: " + provider.getName() +
                " (type: " + provider.getAIG_ProviderType() + ")");

        // Get the existing IAIProvider to extract configuration
        IAIProvider aiProvider = AIProviderFactory.getProvider(ctx, providerId);
        if (aiProvider == null) {
            throw new IllegalStateException("Could not create provider: " + providerId);
        }

        // Create ChatLanguageModel based on provider type
        ChatLanguageModel chatModel = createChatModel(provider, aiProvider);

        String agentName = "agent-" + provider.getValue();
        return new LangChain4jAgent(agentName, chatModel);
    }

    /**
     * Create ChatLanguageModel from iDempiere provider configuration
     *
     * <p>This method bridges iDempiere's provider configuration to LangChain4j.
     * Currently supports wrapping existing IAIProvider implementations.
     *
     * <p>For native LangChain4j support, additional dependencies would be needed:
     * <ul>
     *   <li>langchain4j-anthropic for direct Claude support</li>
     *   <li>langchain4j-open-ai for OpenAI support</li>
     *   <li>langchain4j-bedrock for AWS Bedrock support</li>
     * </ul>
     *
     * @param provider iDempiere provider model
     * @param aiProvider initialized IAIProvider
     * @return ChatLanguageModel instance
     */
    private static ChatLanguageModel createChatModel(MAIGProvider provider, IAIProvider aiProvider) {
        String providerType = provider.getAIG_ProviderType();

        // Create an adapter that wraps our IAIProvider as a ChatLanguageModel
        // This allows using existing provider implementations with LangChain4j
        return new IAIProviderChatModelAdapter(aiProvider, provider);
    }

    /**
     * Builder for creating agents with custom configuration
     */
    public static class Builder {

        private String name;
        private ChatLanguageModel chatModel;
        private IAITools tools;
        private int memorySize = 20;
        private String systemPrompt;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder chatModel(ChatLanguageModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }

        public Builder tools(IAITools tools) {
            this.tools = tools;
            return this;
        }

        public Builder memorySize(int memorySize) {
            this.memorySize = memorySize;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public LangChain4jAgent build() {
            if (name == null || name.isEmpty()) {
                name = "langchain4j-agent";
            }
            if (chatModel == null) {
                throw new IllegalStateException("ChatLanguageModel is required");
            }

            LangChain4jAgent agent = tools != null
                ? new LangChain4jAgent(name, chatModel, tools)
                : new LangChain4jAgent(name, chatModel);

            agent.setMemorySize(memorySize);

            if (systemPrompt != null) {
                agent.setSystemPrompt(systemPrompt);
            }

            return agent;
        }
    }

    /**
     * Create a new builder
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
}
