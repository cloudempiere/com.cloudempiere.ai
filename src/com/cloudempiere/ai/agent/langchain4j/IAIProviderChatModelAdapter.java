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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.compiere.util.CLogger;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.AIProviderException;
import com.cloudempiere.ai.provider.IAIProvider;
import com.cloudempiere.ai.provider.dto.AIFunction;
import com.cloudempiere.ai.provider.dto.AIFunctionCall;
import com.cloudempiere.ai.provider.dto.AIMessage;
import com.cloudempiere.ai.provider.dto.AIRequest;
import com.cloudempiere.ai.provider.dto.AIResponse;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;

/**
 * Adapter that wraps iDempiere's IAIProvider as a LangChain4j ChatLanguageModel
 *
 * <p>This adapter bridges the gap between the existing iDempiere AI provider
 * infrastructure and LangChain4j's model interface. It allows using any
 * configured IAIProvider (Anthropic, AWS Bedrock, OpenAI, etc.) with
 * LangChain4j's agent framework.
 *
 * <p>Features:
 * <ul>
 *   <li>Converts LangChain4j messages to IAIProvider format</li>
 *   <li>Converts IAIProvider responses back to LangChain4j format</li>
 *   <li>Supports tool/function calling</li>
 *   <li>Preserves token usage and cost information</li>
 * </ul>
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class IAIProviderChatModelAdapter implements ChatLanguageModel {

    private static final CLogger log = CLogger.getCLogger(IAIProviderChatModelAdapter.class);

    /** The underlying iDempiere AI provider */
    private final IAIProvider provider;

    /** Provider configuration */
    private final MAIProvider providerConfig;

    /** Default max tokens */
    private int maxTokens = 4096;

    /**
     * Create adapter for an IAIProvider
     *
     * @param provider iDempiere AI provider
     * @param providerConfig provider configuration
     */
    public IAIProviderChatModelAdapter(IAIProvider provider, MAIProvider providerConfig) {
        this.provider = provider;
        this.providerConfig = providerConfig;
    }

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        try {
            // Convert LangChain4j request to IAIProvider request
            AIRequest aiRequest = convertRequest(chatRequest);

            // Get tool specifications if any
            List<ToolSpecification> toolSpecs = chatRequest.toolSpecifications();
            List<AIFunction> functions = convertToolSpecs(toolSpecs);

            // Execute through provider
            AIResponse aiResponse;
            if (functions != null && !functions.isEmpty()) {
                aiResponse = provider.generateTextWithFunctions(aiRequest, functions);
            } else {
                aiResponse = provider.generateText(aiRequest);
            }

            // Convert response back to LangChain4j format
            return convertResponse(aiResponse);

        } catch (AIProviderException e) {
            log.log(Level.SEVERE, "Provider error in chat", e);
            throw new RuntimeException("AI Provider error: " + e.getMessage(), e);
        }
    }

    public Response<AiMessage> generate(List<ChatMessage> messages) {
        ChatRequest request = ChatRequest.builder()
            .messages(messages)
            .build();

        ChatResponse response = chat(request);
        return Response.from(
            response.aiMessage(),
            response.tokenUsage(),
            response.finishReason()
        );
    }

    public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
        ChatRequest request = ChatRequest.builder()
            .messages(messages)
            .toolSpecifications(toolSpecifications)
            .build();

        ChatResponse response = chat(request);
        return Response.from(
            response.aiMessage(),
            response.tokenUsage(),
            response.finishReason()
        );
    }

    public Response<AiMessage> generate(List<ChatMessage> messages, ToolSpecification toolSpecification) {
        List<ToolSpecification> specs = new ArrayList<>();
        if (toolSpecification != null) {
            specs.add(toolSpecification);
        }
        return generate(messages, specs);
    }

    /**
     * Convert LangChain4j ChatRequest to IAIProvider AIRequest
     */
    private AIRequest convertRequest(ChatRequest chatRequest) {
        AIRequest aiRequest = new AIRequest();

        // Set model if available from request parameters
        ChatRequestParameters params = chatRequest.parameters();
        if (params != null && params.modelName() != null) {
            aiRequest.setModel(params.modelName());
        }
        // Note: Model version is typically set by the provider during initialization

        // Set max tokens
        if (params != null && params.maxOutputTokens() != null) {
            aiRequest.setMaxTokens(params.maxOutputTokens());
        } else {
            aiRequest.setMaxTokens(maxTokens);
        }

        // Set temperature
        if (params != null && params.temperature() != null) {
            aiRequest.setTemperature(params.temperature());
        }

        // Convert messages
        List<AIMessage> aiMessages = new ArrayList<>();
        for (ChatMessage message : chatRequest.messages()) {
            AIMessage aiMsg = convertMessage(message);
            if (aiMsg != null) {
                // Handle system message separately
                if (message instanceof SystemMessage) {
                    aiRequest.setSystemPrompt(((SystemMessage) message).text());
                } else {
                    aiMessages.add(aiMsg);
                }
            }
        }
        aiRequest.setMessages(aiMessages);

        return aiRequest;
    }

    /**
     * Convert a single LangChain4j message to IAIProvider format
     */
    private AIMessage convertMessage(ChatMessage message) {
        if (message instanceof UserMessage) {
            UserMessage userMsg = (UserMessage) message;
            return new AIMessage("user", userMsg.singleText());

        } else if (message instanceof AiMessage) {
            AiMessage aiMsg = (AiMessage) message;
            return new AIMessage("assistant", aiMsg.text());

        } else if (message instanceof SystemMessage) {
            // System messages are handled separately
            return null;

        } else if (message instanceof ToolExecutionResultMessage) {
            ToolExecutionResultMessage toolMsg = (ToolExecutionResultMessage) message;
            AIMessage msg = new AIMessage("user", toolMsg.text());
            msg.setName(toolMsg.toolName());
            return msg;
        }

        log.warning("Unknown message type: " + message.getClass().getName());
        return null;
    }

    /**
     * Convert LangChain4j ToolSpecifications to IAIProvider AIFunctions
     */
    private List<AIFunction> convertToolSpecs(List<ToolSpecification> toolSpecs) {
        if (toolSpecs == null || toolSpecs.isEmpty()) {
            return null;
        }

        List<AIFunction> functions = new ArrayList<>();
        for (ToolSpecification spec : toolSpecs) {
            AIFunction func = new AIFunction();
            func.setName(spec.name());
            func.setDescription(spec.description());

            // Convert parameters schema - store as Map with schema string
            if (spec.parameters() != null) {
                Map<String, Object> paramsMap = new HashMap<>();
                paramsMap.put("schema", spec.parameters().toString());
                func.setParameters(paramsMap);
            }

            functions.add(func);
        }

        return functions;
    }

    /**
     * Convert IAIProvider AIResponse to LangChain4j ChatResponse
     */
    private ChatResponse convertResponse(AIResponse aiResponse) {
        // Build AiMessage
        AiMessage.Builder aiMsgBuilder = AiMessage.builder();

        // Set text content if present
        if (aiResponse.getContent() != null && !aiResponse.getContent().isEmpty()) {
            aiMsgBuilder.text(aiResponse.getContent());
        }

        // Convert function calls to tool execution requests
        List<AIFunctionCall> functionCalls = aiResponse.getFunctionCalls();
        if (functionCalls != null && !functionCalls.isEmpty()) {
            List<ToolExecutionRequest> toolRequests = new ArrayList<>();

            for (AIFunctionCall call : functionCalls) {
                ToolExecutionRequest request = ToolExecutionRequest.builder()
                    .id(call.getId() != null ? call.getId() : call.getName())
                    .name(call.getName())
                    .arguments(call.getArguments())
                    .build();
                toolRequests.add(request);
            }

            aiMsgBuilder.toolExecutionRequests(toolRequests);
        }

        AiMessage aiMessage = aiMsgBuilder.build();

        // Build token usage
        TokenUsage tokenUsage = null;
        if (aiResponse.getTokenUsage() != null) {
            tokenUsage = new TokenUsage(
                aiResponse.getTokenUsage().getPromptTokens(),
                aiResponse.getTokenUsage().getCompletionTokens()
            );
        }

        // Build ChatResponse
        return ChatResponse.builder()
            .aiMessage(aiMessage)
            .tokenUsage(tokenUsage)
            .build();
    }

    /**
     * Set the maximum tokens for responses
     *
     * @param maxTokens max tokens
     */
    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    /**
     * Get the underlying provider
     *
     * @return IAIProvider instance
     */
    public IAIProvider getProvider() {
        return provider;
    }

    /**
     * Get provider configuration
     *
     * @return MAIProvider instance
     */
    public MAIProvider getProviderConfig() {
        return providerConfig;
    }
}
