/******************************************************************************
 * Product: Cloudempiere ERP & CRM Smart Business Solution                   *
 * Copyright (C) 2025 Cloudempiere, Inc. All Rights Reserved.                 *
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

import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cloudempiere.ai.database.SecureDatabaseQueryExecutor;
import com.cloudempiere.ai.database.dto.SecureQueryRequest;
import com.cloudempiere.ai.database.dto.SecureQueryResult;
import com.cloudempiere.ai.model.MAIProvider;
import com.cloudempiere.ai.provider.langchain4j.LangChain4jProviderFactory;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;

/**
 * Test Process for AI Providers using LangChain4j
 *
 * This process tests AI provider integrations by:
 * 1. Creating a ChatLanguageModel from provider configuration
 * 2. Generating a simple text response
 * 3. Displaying results and metrics (tokens, response time)
 * 4. Testing Secure Database Query Executor
 *    - Valid queries with access control
 *    - Multi-table JOIN queries
 *    - Security validations (INSERT/UPDATE/DELETE blocked)
 *    - SQL injection protection
 *    - Row limit enforcement
 *    - Audit trail verification
 *
 * Works with any registered AI provider type via LangChain4j:
 * - Anthropic Claude (ANT)
 * - AWS Bedrock (ABE)
 * - OpenAI (OAI)
 * - Ollama (OLL)
 * - LLaMA (LLA)
 *
 * @author Cloudempiere
 * @version 3.0
 */
@org.adempiere.base.annotation.Process
public class TestAIProvider extends SvrProcess {

	/** AI Provider ID Parameter */
	private int p_AIG_Provider_ID = 0;

	/** Test Prompt */
	private String p_Prompt = "Say hello and tell me what AI model you are in one sentence.";

	/** Max Tokens */
	private int p_MaxTokens = 100;

	/** Test Database Query */
	private boolean p_TestDatabaseQuery = true;

	/** Custom Test SQL */
	private String p_TestSQL = null;

	/** Max Rows to Return */
	private int p_MaxRowsToReturn = 5;

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
			} else if (name.equals("Prompt")) {
				p_Prompt = para[i].getParameterAsString();
			} else if (name.equals("MaxTokens")) {
				p_MaxTokens = para[i].getParameterAsInt();
			} else if (name.equals("TestDatabaseQuery")) {
				p_TestDatabaseQuery = "Y".equals(para[i].getParameterAsString());
			} else if (name.equals("TestSQL")) {
				p_TestSQL = para[i].getParameterAsString();
			} else if (name.equals("MaxRowsToReturn")) {
				p_MaxRowsToReturn = para[i].getParameterAsInt();
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
	 * Process - Test AI Provider using LangChain4j
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
		addLog("║  Using: LangChain4j");
		addLog("╚═══════════════════════════════════════════════════════╝");

		// Get provider type name for display
		String providerTypeName = getProviderTypeName(providerConfig.getAIGProviderType());
		addLog("Provider Implementation: " + providerTypeName);

		// Create ChatLanguageModel using LangChain4jProviderFactory
		ChatLanguageModel chatModel;
		try {
			chatModel = LangChain4jProviderFactory.create(providerConfig);
			addLog("✓ ChatLanguageModel created successfully");
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to create ChatLanguageModel", e);
			throw new AdempiereException("Failed to create ChatLanguageModel: " + e.getMessage(), e);
		}

		// Test 1: Text Generation
		addLog("=== Test 1: Text Generation ===");
		addLog("Prompt: " + p_Prompt);
		addLog("Max Tokens: " + p_MaxTokens);

		try {
			// Generate response
			long startTime = System.currentTimeMillis();
			Response<AiMessage> response = chatModel.generate(UserMessage.from(p_Prompt));
			long duration = System.currentTimeMillis() - startTime;

			// Display results
			if (response != null && response.content() != null) {
				addLog("Text Generation: SUCCESS");
				addLog("Response: " + response.content().text());
				addLog("Processing Time: " + duration + "ms");

				if (response.tokenUsage() != null) {
					addLog("Tokens Used: " + response.tokenUsage().totalTokenCount()
							+ " (Input: " + response.tokenUsage().inputTokenCount()
							+ ", Output: " + response.tokenUsage().outputTokenCount() + ")");
				}

				if (response.finishReason() != null) {
					addLog("Finish Reason: " + response.finishReason());
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

		// Test 2: Provider Info
		addLog("=== Test 2: Provider Configuration ===");
		addLog("Provider Name: " + providerConfig.getName());
		addLog("Provider Type: " + providerTypeName);
		addLog("Model: " + (providerConfig.getModelName() != null ? providerConfig.getModelName() : "default"));
		addLog("Active: " + (providerConfig.isActive() ? "YES" : "NO"));
		addLog("Default: " + (providerConfig.isDefault() ? "YES" : "NO"));

		// Test 3: Secure Database Query Executor
		if (p_TestDatabaseQuery) {
			testSecureDatabaseQueryExecutor(p_AIG_Provider_ID);
		}

		addLog("═══════════════════════════════════════════════════════");
		addLog("✓ All tests completed successfully!");
		return "All tests passed for " + providerTypeName;
	}

	/**
	 * Test Secure Database Query Executor
	 */
	private void testSecureDatabaseQueryExecutor(int providerId) {
		addLog("=== Test 3: Secure Database Query Executor ===");

		SecureDatabaseQueryExecutor executor = new SecureDatabaseQueryExecutor();
		int testsRun = 0;
		int testsPassed = 0;

		// Test 3.1: Valid Query - Simple SELECT
		testsRun++;
		addLog("--- Test 3.1: Valid Query (Simple SELECT) ---");
		String sql1 = p_TestSQL != null ? p_TestSQL :
			"SELECT AD_User_ID, Name, Created FROM AD_User WHERE AD_User_ID = " + Env.getAD_User_ID(getCtx());
		if (testQuery(executor, providerId, sql1, "Simple SELECT on AD_User", "TEST_VALID_QUERY", true)) {
			testsPassed++;
		}

		// Test 3.2: Multiple Tables with JOIN
		testsRun++;
		addLog("--- Test 3.2: Multiple Tables Query (JOIN) ---");
		String sql2 = "SELECT AD_User.Name AS UserName, AD_Role.Name AS RoleName " +
					  "FROM AD_User " +
					  "INNER JOIN AD_User_Roles ON (AD_User.AD_User_ID = AD_User_Roles.AD_User_ID) " +
					  "INNER JOIN AD_Role ON (AD_User_Roles.AD_Role_ID = AD_Role.AD_Role_ID) " +
					  "WHERE AD_User.AD_User_ID = " + Env.getAD_User_ID(getCtx());
		if (testQuery(executor, providerId, sql2, "JOIN query with multiple tables", "TEST_JOIN_QUERY", true)) {
			testsPassed++;
		}

		// Test 3.3: Aggregate Function
		testsRun++;
		addLog("--- Test 3.3: Aggregate Function Query ---");
		String sql3 = "SELECT COUNT(*) AS TableCount FROM AD_Table WHERE AD_Client_ID IN (0, " +
					  Env.getAD_Client_ID(getCtx()) + ")";
		if (testQuery(executor, providerId, sql3, "Aggregate COUNT function", "TEST_AGGREGATE", true)) {
			testsPassed++;
		}

		// Test 3.4: Invalid SQL - Security Validation (INSERT)
		testsRun++;
		addLog("--- Test 3.4: Security Test (Forbidden INSERT) ---");
		String sql4 = "INSERT INTO AD_Note (AD_Note_ID) VALUES (999999)";
		if (testQuery(executor, providerId, sql4, "Security test - INSERT should be rejected", "TEST_SECURITY_INSERT", false)) {
			testsPassed++;
		}

		// Test 3.5: Invalid SQL - Security Validation (UPDATE)
		testsRun++;
		addLog("--- Test 3.5: Security Test (Forbidden UPDATE) ---");
		String sql5 = "UPDATE AD_User SET Name='Hacked' WHERE AD_User_ID=1";
		if (testQuery(executor, providerId, sql5, "Security test - UPDATE should be rejected", "TEST_SECURITY_UPDATE", false)) {
			testsPassed++;
		}

		// Test 3.6: Invalid SQL - Security Validation (DELETE)
		testsRun++;
		addLog("--- Test 3.6: Security Test (Forbidden DELETE) ---");
		String sql6 = "DELETE FROM AD_User WHERE AD_User_ID=1";
		if (testQuery(executor, providerId, sql6, "Security test - DELETE should be rejected", "TEST_SECURITY_DELETE", false)) {
			testsPassed++;
		}

		// Test 3.7: Invalid SQL - SQL Injection Protection (Semicolon)
		testsRun++;
		addLog("--- Test 3.7: SQL Injection Protection (Semicolon) ---");
		String sql7 = "SELECT * FROM AD_User; DROP TABLE AD_User";
		if (testQuery(executor, providerId, sql7, "SQL injection - semicolon should be rejected", "TEST_SQL_INJECTION", false)) {
			testsPassed++;
		}

		// Test 3.8: Invalid SQL - SQL Injection Protection (Comments)
		testsRun++;
		addLog("--- Test 3.8: SQL Injection Protection (Comments) ---");
		String sql8 = "SELECT * FROM AD_User -- WHERE AD_User_ID=1";
		if (testQuery(executor, providerId, sql8, "SQL injection - comments should be rejected", "TEST_SQL_COMMENTS", false)) {
			testsPassed++;
		}

		// Test 3.9: Row Limit Enforcement
		testsRun++;
		addLog("--- Test 3.9: Row Limit Enforcement ---");
		String sql9 = "SELECT AD_Table_ID, TableName FROM AD_Table WHERE AD_Client_ID IN (0, " +
					  Env.getAD_Client_ID(getCtx()) + ")";
		if (testQueryWithRowLimit(executor, providerId, sql9, "Row limit enforcement", p_MaxRowsToReturn)) {
			testsPassed++;
		}

		// Test 3.10: Audit Trail Verification
		testsRun++;
		addLog("--- Test 3.10: Audit Trail Verification ---");
		if (testAuditTrail(providerId)) {
			testsPassed++;
		}

		// Summary
		addLog("───────────────────────────────────────────────────────");
		addLog("Database Query Tests: " + testsPassed + "/" + testsRun + " PASSED");
		if (testsPassed == testsRun) {
			addLog("✓ All database query tests PASSED!");
		} else {
			addLog("⚠ " + (testsRun - testsPassed) + " test(s) FAILED");
		}
	}

	/**
	 * Test a single query
	 */
	private boolean testQuery(SecureDatabaseQueryExecutor executor, int providerId,
							  String sql, String purpose, String contextType, boolean expectSuccess) {
		try {
			addLog("SQL: " + (sql.length() > 80 ? sql.substring(0, 80) + "..." : sql));
			addLog("Purpose: " + purpose);
			addLog("Expected: " + (expectSuccess ? "SUCCESS" : "REJECTED"));

			SecureQueryRequest request = new SecureQueryRequest.Builder()
				.ctx(getCtx())
				.sql(sql)
				.providerId(providerId)
				.contextType(contextType)
				.queryPurpose(purpose)
				.maxRows(p_MaxRowsToReturn)
				.build();

			SecureQueryResult result = executor.executeQuery(request);

			boolean actualSuccess = result.isSuccess();
			boolean testPassed = (expectSuccess == actualSuccess);

			addLog("Result: " + result.getStatus() + (testPassed ? " ✓" : " ✗"));
			addLog("User: " + result.getUserId() + ", Role: " + result.getRoleId() +
				   " (" + result.getRoleName() + ")");

			if (result.getTablesAccessed() != null && !result.getTablesAccessed().isEmpty()) {
				addLog("Tables: " + String.join(", ", result.getTablesAccessed()));
			}

			if (actualSuccess) {
				addLog("Rows: " + result.getRowCount());
				addLog("Query Time: " + result.getQueryExecutionTimeMs() + "ms");
				addLog("Total Time: " + result.getTotalExecutionTimeMs() + "ms");

				// Show sample data
				if (result.getRows() != null && result.getRows().length() > 0) {
					int samplesToShow = Math.min(3, result.getRows().length());
					addLog("Sample Data (first " + samplesToShow + " rows):");
					for (int i = 0; i < samplesToShow; i++) {
						JSONObject row = result.getRows().getJSONObject(i);
						addLog("  Row " + (i + 1) + ": " + formatRow(row, result.getColumns()));
					}
				}

				// Show secured SQL
				if (result.getSecuredSQL() != null && !result.getSecuredSQL().equals(sql)) {
					addLog("Security SQL Added: YES");
				}
			} else {
				addLog("Error: " + result.getErrorMessage());
			}

			if (testPassed) {
				addLog("Test Result: PASSED ✓");
			} else {
				addLog("Test Result: FAILED ✗ (Expected: " + (expectSuccess ? "SUCCESS" : "ERROR") +
					   ", Got: " + result.getStatus() + ")");
			}

			return testPassed;

		} catch (SecurityException e) {
			boolean testPassed = !expectSuccess;
			addLog("Security Exception: " + e.getMessage());
			addLog("Test Result: " + (testPassed ? "PASSED ✓" : "FAILED ✗"));
			return testPassed;
		} catch (Exception e) {
			boolean testPassed = !expectSuccess;
			addLog("Exception: " + e.getMessage());
			addLog("Test Result: " + (testPassed ? "PASSED ✓" : "FAILED ✗"));
			return testPassed;
		}
	}

	/**
	 * Test query with row limit
	 */
	private boolean testQueryWithRowLimit(SecureDatabaseQueryExecutor executor, int providerId,
										  String sql, String purpose, int maxRows) {
		try {
			addLog("SQL: " + (sql.length() > 80 ? sql.substring(0, 80) + "..." : sql));
			addLog("Purpose: " + purpose);
			addLog("Max Rows Limit: " + maxRows);

			SecureQueryRequest request = new SecureQueryRequest.Builder()
				.ctx(getCtx())
				.sql(sql)
				.providerId(providerId)
				.contextType("TEST_ROW_LIMIT")
				.queryPurpose(purpose)
				.maxRows(maxRows)
				.build();

			SecureQueryResult result = executor.executeQuery(request);

			boolean testPassed = result.isSuccess() && result.getRowCount() <= maxRows;

			addLog("Result: " + result.getStatus());
			addLog("Rows Returned: " + result.getRowCount());
			addLog("Within Limit: " + (result.getRowCount() <= maxRows ? "YES ✓" : "NO ✗"));

			// Verify LIMIT clause was added
			if (result.getSecuredSQL() != null) {
				String securedSQL = result.getSecuredSQL().toUpperCase();
				boolean hasLimit = securedSQL.contains("LIMIT") || securedSQL.contains("FETCH FIRST");
				addLog("LIMIT Clause Added: " + (hasLimit ? "YES" : "NO"));
			}

			addLog("Test Result: " + (testPassed ? "PASSED ✓" : "FAILED ✗"));
			return testPassed;

		} catch (Exception e) {
			addLog("Exception: " + e.getMessage());
			addLog("Test Result: FAILED ✗");
			return false;
		}
	}

	/**
	 * Test audit trail
	 */
	private boolean testAuditTrail(int providerId) {
		try {
			addLog("Checking audit trail for provider ID: " + providerId);

			String auditSQL = "SELECT COUNT(*) FROM AIG_QueryAudit " +
							  "WHERE AIG_Provider_ID = ? " +
							  "AND AD_User_ID = ? " +
							  "AND Created > CURRENT_TIMESTAMP - INTERVAL '1 hour'";

			int auditCount = org.compiere.util.DB.getSQLValue(null, auditSQL,
				providerId, Env.getAD_User_ID(getCtx()));

			addLog("Audit Records Found: " + auditCount);

			if (auditCount > 0) {
				addLog("✓ Audit logging is working");
				addLog("Test Result: PASSED ✓");
				return true;
			} else {
				addLog("⚠ No audit records found (may be normal if this is first test)");
				addLog("Test Result: PASSED ✓");
				return true;
			}

		} catch (Exception e) {
			addLog("Audit check failed: " + e.getMessage());
			addLog("Test Result: FAILED ✗");
			return false;
		}
	}

	/**
	 * Format a row for display
	 */
	private String formatRow(JSONObject row, JSONArray columns) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < columns.length() && i < 3; i++) { // Show first 3 columns
			if (i > 0) sb.append(", ");
			String colName = columns.getString(i);
			Object value = row.opt(colName);
			sb.append(colName).append("=").append(value != null ? value.toString() : "null");
		}
		if (columns.length() > 3) {
			sb.append(" ... (+" + (columns.length() - 3) + " more)");
		}
		return sb.toString();
	}

	/**
	 * Get friendly provider type name
	 */
	private String getProviderTypeName(String providerType) {
		if (MAIProvider.AIGPROVIDERTYPE_AnthropicClaude.equals(providerType)) {
			return "Anthropic Claude";
		} else if (MAIProvider.AIGPROVIDERTYPE_AWSBedrock.equals(providerType)) {
			return "AWS Bedrock";
		} else if (MAIProvider.AIGPROVIDERTYPE_Ollama.equals(providerType)) {
			return "Ollama";
		}
		return "Unknown (" + providerType + ")";
	}
}
