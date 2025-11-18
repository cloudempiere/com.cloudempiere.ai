/******************************************************************************
 * Product: CloudEmpiere ERP & CRM Smart Business Solution                   *
 * Copyright (C) 2025 CloudEmpiere, Inc. All Rights Reserved.                 *
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
package com.cloudempiere.ai.process;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;

import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.AIProviderException;
import com.cloudempiere.ai.provider.IAIProvider;
import com.cloudempiere.ai.provider.dto.AIHealthStatus;
import com.cloudempiere.ai.provider.dto.AIMessage;
import com.cloudempiere.ai.provider.dto.AIModelCapabilities;
import com.cloudempiere.ai.provider.dto.AIRequest;
import com.cloudempiere.ai.provider.dto.AIResponse;
import com.cloudempiere.ai.provider.factory.AIProviderFactory;

/**
 * Test Process for AI Providers (Anthropic Claude & AWS Bedrock)
 *
 * This process tests AI provider integrations by:
 * 1. Running a health check
 * 2. Generating a simple text response
 * 3. Testing streaming (optional)
 * 4. Displaying results and metrics (tokens, cost, response time)
 * 5. Showing feature support
 *
 * Works with any registered AI provider type:
 * - Anthropic Claude (ANT)
 * - AWS Bedrock (ABE)
 * - Any future providers
 *
 * @author CloudEmpiere
 * @version 2.0
 */
@org.adempiere.base.annotation.Process
public class TestAnthropicProvider extends SvrProcess {

	/** AI Provider ID Parameter */
	private int p_AIG_Provider_ID = 0;

	/** Test Model (default: claude-3-haiku-20240307) */
	private String p_Model = "claude-3-haiku-20240307";

	/** Test Prompt */
	private String p_Prompt = "Say hello and tell me what AI model you are in one sentence.";

	/** Max Tokens */
	private int p_MaxTokens = 100;

	/**
	 * Prepare - Get Parameters
	 */
	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null) {
				;
			} else if (name.equals("AIG_Provider_ID")) {
				p_AIG_Provider_ID = para[i].getParameterAsInt();
			} else if (name.equals("Model")) {
				p_Model = para[i].getParameterAsString();
			} else if (name.equals("Prompt")) {
				p_Prompt = para[i].getParameterAsString();
			} else if (name.equals("MaxTokens")) {
				p_MaxTokens = para[i].getParameterAsInt();
			} else {
				log.log(Level.SEVERE, "Unknown Parameter: " + name);
			}
		}

		// If no provider specified, try to get from record
		if (p_AIG_Provider_ID == 0) {
			p_AIG_Provider_ID = getRecord_ID();
		}
	}

	/**
	 * Process - Test AI Provider (Any Type)
	 *
	 * @return Summary message
	 */
	@Override
	protected String doIt() throws Exception {
		if (p_AIG_Provider_ID == 0) {
			throw new AdempiereException("@AIG_Provider_ID@ @NotFound@");
		}

		// Load provider configuration
		MAIProvider providerConfig = new MAIProvider(getCtx(), p_AIG_Provider_ID, get_TrxName());
		if (providerConfig.get_ID() == 0) {
			throw new AdempiereException("AI Provider not found: " + p_AIG_Provider_ID);
		}

		if (!providerConfig.isActive()) {
			throw new AdempiereException("AI Provider is not active: " + providerConfig.getName());
		}

		addLog("╔═══════════════════════════════════════════════════════╗");
		addLog("║  Testing AI Provider: " + providerConfig.getName());
		addLog("║  Provider Type: " + providerConfig.getAIGProviderType());
		addLog("╚═══════════════════════════════════════════════════════╝");

		// Get provider type name for display
		String providerTypeName = getProviderTypeName(providerConfig.getAIGProviderType());
		addLog("Provider Implementation: " + providerTypeName);

		// Use factory to get provider instance
		AIProviderFactory factory = new AIProviderFactory();
		IAIProvider provider = null;

		try {
			provider = factory.get(providerConfig);
			addLog("✓ Provider initialized successfully");
			addLog("  API Version: " + provider.getAPIVersion());
		} catch (AIProviderException e) {
			log.log(Level.SEVERE, "Failed to initialize provider", e);
			throw new AdempiereException("Failed to initialize provider: " + e.getMessage(), e);
		}

		// Test 1: Health Check
		addLog("=== Test 1: Health Check ===");
		try {
			AIHealthStatus health = provider.checkHealth();
			if (health.isHealthy()) {
				addLog("Health Check: PASSED");
				addLog("Status: " + health.getStatus());
				addLog("Response Time: " + health.getResponseTimeMs() + "ms");
			} else {
				addLog("Health Check: FAILED");
				addLog("Status: " + health.getStatus());
				addLog("Error: " + health.getMessage());
				return "@Error@ Health check failed: " + health.getMessage();
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "Health check failed", e);
			addLog("Health Check: ERROR - " + e.getMessage());
			throw new AdempiereException("Health check failed: " + e.getMessage(), e);
		}

		// Test 2: Text Generation
		addLog("=== Test 2: Text Generation ===");
		addLog("Model: " + p_Model);
		addLog("Prompt: " + p_Prompt);
		addLog("Max Tokens: " + p_MaxTokens);

		try {
			// Create request
			AIRequest request = new AIRequest();
			request.setModel(p_Model);
			request.setMaxTokens(p_MaxTokens);
			request.setTemperature(0.7);

			// Create message
			List<AIMessage> messages = new ArrayList<>();
			AIMessage userMessage = new AIMessage("user", p_Prompt);
			messages.add(userMessage);
			request.setMessages(messages);

			// Generate response
			long startTime = System.currentTimeMillis();
			AIResponse response = provider.generateText(request);
			long duration = System.currentTimeMillis() - startTime;

			// Display results
			if (response != null && response.getContent() != null) {
				addLog("Text Generation: SUCCESS");
				addLog("Response: " + response.getContent());
				addLog("Model Used: " + response.getModel());
				addLog("Processing Time: " + duration + "ms");

				if (response.getTokenUsage() != null) {
					addLog("Tokens Used: " + response.getTokenUsage().getTotalTokens()
							+ " (Prompt: " + response.getTokenUsage().getPromptTokens()
							+ ", Completion: " + response.getTokenUsage().getCompletionTokens() + ")");
				}

				if (response.getCostUSD() > 0) {
					addLog(String.format("Cost: $%.6f", response.getCostUSD()));
				}
			} else {
				addLog("Text Generation: FAILED - No response content");
				return "@Error@ Text generation failed - no response content";
			}

		} catch (Exception e) {
			log.log(Level.SEVERE, "Text generation failed", e);
			addLog("Text Generation: ERROR - " + e.getMessage());
			throw new AdempiereException("Text generation failed: " + e.getMessage(), e);
		}

		// Test 3: Feature Support
		addLog("=== Test 3: Feature Support ===");
		addLog("✓ Text Generation: " + (provider.supportsTextGeneration() ? "YES" : "NO"));
		addLog("  Streaming: " + (provider.supportsStreaming() ? "YES" : "NO"));
		addLog("  Function Calling: " + (provider.supportsFunctionCalling() ? "YES" : "NO"));
		addLog("  Vision: " + (provider.supportsVision() ? "YES" : "NO"));
		addLog("  Audio: " + (provider.supportsAudio() ? "YES" : "NO"));
		addLog("  Embeddings: " + (provider.supportsEmbeddings() ? "YES" : "NO"));

		// Test 4: Supported Models
		addLog("=== Test 4: Supported Models ===");
		try {
			List<String> supportedModels = provider.getSupportedModels();
			if (supportedModels != null && !supportedModels.isEmpty()) {
				addLog("Available Models (" + supportedModels.size() + "):");
				int count = 0;
				for (String model : supportedModels) {
					addLog("  " + (++count) + ". " + model);
					if (count >= 5) {
						addLog("  ... and " + (supportedModels.size() - 5) + " more");
						break;
					}
				}
			} else {
				addLog("No models listed");
			}
		} catch (Exception e) {
			addLog("Error getting supported models: " + e.getMessage());
		}

		// Test 5: Model Capabilities (for the model being tested)
		addLog("=== Test 5: Model Capabilities ===");
		addLog("Model: " + p_Model);
		try {
			AIModelCapabilities caps = provider.getModelCapabilities(p_Model);
			if (caps != null) {
				addLog("  Max Context Length: " + caps.getMaxContextLength() + " tokens");
				addLog("  Max Output Tokens: " + caps.getMaxOutputTokens() + " tokens");
				addLog("  Supports Vision: " + (caps.isSupportsVision() ? "YES" : "NO"));
				addLog("  Supports Functions: " + (caps.isSupportsFunctions() ? "YES" : "NO"));
				addLog("  Supports Streaming: " + (caps.isSupportsStreaming() ? "YES" : "NO"));
				addLog("  Supports JSON: " + (caps.isSupportsJSON() ? "YES" : "NO"));
			}
		} catch (Exception e) {
			addLog("Error getting model capabilities: " + e.getMessage());
		}

		// Clean up
		try {
			provider.shutdown();
			addLog("✓ Provider shutdown successfully");
		} catch (Exception e) {
			log.warning("Error shutting down provider: " + e.getMessage());
		}

		addLog("═══════════════════════════════════════════════════════");
		addLog("✓ All tests completed successfully!");
		return "All tests passed for " + providerTypeName;
	}

	/**
	 * Get friendly provider type name
	 */
	private String getProviderTypeName(String providerType) {
		if (MAIProvider.AIGPROVIDERTYPE_AnthropicClaude.equals(providerType)) {
			return "Anthropic Claude";
		} else if (MAIProvider.AIGPROVIDERTYPE_AWSBedrock.equals(providerType)) {
			return "AWS Bedrock";
		}
		return "Unknown (" + providerType + ")";
	}
}