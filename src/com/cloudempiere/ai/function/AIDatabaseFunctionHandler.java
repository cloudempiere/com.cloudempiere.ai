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
package com.cloudempiere.ai.function;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cloudempiere.ai.database.SecureDatabaseQueryExecutor;
import com.cloudempiere.ai.database.dto.SecureQueryRequest;
import com.cloudempiere.ai.database.dto.SecureQueryResult;
import com.cloudempiere.ai.provider.dto.AIFunction;

/**
 * Handler for AI database query function calls
 *
 * <p>This class processes function calls from AI providers when they
 * need to query the database for information.
 *
 * <p><b>Security Model:</b>
 * <ul>
 *   <li>Only SELECT queries are allowed</li>
 *   <li>User's role permissions are enforced</li>
 *   <li>All queries are audited</li>
 *   <li>Sensitive columns are redacted</li>
 * </ul>
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AIDatabaseFunctionHandler {

	private static final CLogger log = CLogger.getCLogger(AIDatabaseFunctionHandler.class);

	/** Database query executor */
	private final SecureDatabaseQueryExecutor executor;

	/**
	 * Default constructor
	 */
	public AIDatabaseFunctionHandler() {
		this.executor = new SecureDatabaseQueryExecutor();
	}

	/**
	 * Execute database query function call
	 *
	 * @param ctx User context (for role-based security)
	 * @param providerId AI provider ID (for audit trail)
	 * @param arguments Function arguments as JSON string
	 * @return Query result as JSON string
	 */
	public String executeQueryFunction(Properties ctx, int providerId, String arguments) {
		try {
			// Parse function arguments
			JSONObject args = new JSONObject(arguments);
			String sql = args.optString("sql", null);
			String purpose = args.optString("purpose", "AI assistant query");
			int maxRows = args.optInt("max_rows", 0);

			if (sql == null || sql.trim().isEmpty()) {
				return createErrorResult("SQL query is required");
			}

			log.fine("AI Database Function Call - SQL: " + sql + ", Purpose: " + purpose);

			// Build secure query request
			SecureQueryRequest request = new SecureQueryRequest.Builder()
				.ctx(ctx)
				.sql(sql)
				.providerId(providerId)
				.contextType("CHAT")
				.queryPurpose(purpose)
				.maxRows(maxRows > 0 ? Math.min(maxRows, 100) : 50) // Default 50, max 100
				.timeoutMs(5000) // 5 second timeout for chat queries
				.build();

			// Execute query
			SecureQueryResult result = executor.executeQuery(request);

			// Return result as JSON
			return result.toJSON().toString();

		} catch (SecurityException e) {
			log.log(Level.WARNING, "Security violation in AI query", e);
			return createErrorResult("Access denied: " + e.getMessage());

		} catch (Exception e) {
			log.log(Level.SEVERE, "Error executing AI database function", e);
			return createErrorResult("Query failed: " + e.getMessage());
		}
	}

	/**
	 * Create error result JSON
	 *
	 * @param errorMessage Error message
	 * @return JSON error result
	 */
	private String createErrorResult(String errorMessage) {
		JSONObject error = new JSONObject();
		error.put("status", "ERROR");
		error.put("error", errorMessage);
		error.put("rows", new JSONArray());
		error.put("row_count", 0);
		return error.toString();
	}

	/**
	 * Get function definition for AI providers
	 *
	 * <p>This defines the schema that the AI model will use to understand
	 * when and how to call the database query function.
	 *
	 * @return AIFunction definition
	 */
	public static AIFunction getDatabaseQueryFunction() {
		AIFunction function = new AIFunction();
		function.setName("query_database");
		function.setDescription(
			"Query the iDempiere database to retrieve business data. " +
			"Use this when the user asks questions that require data from the system " +
			"(e.g., 'show me orders', 'what products do we have', 'find customer X'). " +
			"Only SELECT queries are allowed. The query will be executed with the " +
			"user's role permissions and security will be automatically applied."
		);

		// Define parameters using JSON Schema
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("type", "object");

		Map<String, Object> properties = new HashMap<>();

		// SQL parameter
		Map<String, Object> sqlParam = new HashMap<>();
		sqlParam.put("type", "string");
		sqlParam.put("description",
			"SELECT SQL query to execute. Must be read-only (no INSERT/UPDATE/DELETE). " +
			"Use standard iDempiere table names (e.g., C_Order, C_BPartner, M_Product). " +
			"Example: SELECT Name, Value FROM M_Product WHERE IsActive='Y' LIMIT 10");
		properties.put("sql", sqlParam);

		// Purpose parameter (optional)
		Map<String, Object> purposeParam = new HashMap<>();
		purposeParam.put("type", "string");
		purposeParam.put("description",
			"Brief description of why this query is needed (for audit trail)");
		properties.put("purpose", purposeParam);

		// Max rows parameter (optional)
		Map<String, Object> maxRowsParam = new HashMap<>();
		maxRowsParam.put("type", "integer");
		maxRowsParam.put("description", "Maximum number of rows to return (default: 50, max: 100)");
		maxRowsParam.put("minimum", 1);
		maxRowsParam.put("maximum", 100);
		properties.put("max_rows", maxRowsParam);

		parameters.put("properties", properties);
		parameters.put("required", Arrays.asList("sql"));

		function.setParameters(parameters);
		return function;
	}
}
