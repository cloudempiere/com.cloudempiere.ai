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
package com.cloudempiere.ai.tool;

import java.util.Map;

import com.cloudempiere.ai.agent.AgentContext;

/**
 * Interface for AI Agent Tools
 *
 * <p>Tools are the bridge between AI agents and iDempiere functionality.
 * Each tool provides a specific capability that the AI can invoke to
 * accomplish tasks.
 *
 * <p>Tool implementations must:
 * <ul>
 *   <li>Respect security boundaries (use existing security layers)</li>
 *   <li>Validate all inputs before execution</li>
 *   <li>Return results in a format the AI can understand</li>
 *   <li>Handle errors gracefully and return meaningful error messages</li>
 * </ul>
 *
 * <p>Example tools:
 * <ul>
 *   <li>DatabaseQueryTool - Execute SQL queries</li>
 *   <li>FieldMetadataTool - Get field metadata from AD_Field</li>
 *   <li>TableMetadataTool - Get table metadata from AD_Table</li>
 *   <li>ProcessExecutionTool - Run iDempiere processes</li>
 * </ul>
 *
 * @author Cloudempiere
 * @version 1.0
 */
public interface ITool {

    /**
     * Get the unique name of this tool
     *
     * <p>This name is used by the AI to identify and call the tool.
     * Should be descriptive and use snake_case format.
     *
     * <p>Examples: "query_database", "get_field_metadata", "run_process"
     *
     * @return tool name (unique identifier)
     */
    String getName();

    /**
     * Get a human-readable description of what this tool does
     *
     * <p>This description is provided to the AI to help it understand
     * when and how to use the tool. Should be clear and concise.
     *
     * <p>Example: "Execute a read-only SQL SELECT query against the
     * iDempiere database with automatic security filtering"
     *
     * @return tool description
     */
    String getDescription();

    /**
     * Get the parameters this tool accepts
     *
     * <p>Returns a map of parameter names to their descriptions.
     * This information is used to generate the tool schema for the AI.
     *
     * <p>Example:
     * <pre>
     * {
     *   "sql": "The SQL SELECT query to execute",
     *   "max_rows": "Maximum number of rows to return (default: 100)"
     * }
     * </pre>
     *
     * @return map of parameter name to description
     */
    Map<String, ToolParameter> getParameters();

    /**
     * Execute the tool with the given parameters
     *
     * <p>This method is called by the agent when it decides to use this tool.
     * Implementations should:
     * <ul>
     *   <li>Validate all inputs</li>
     *   <li>Use the AgentContext for security and audit</li>
     *   <li>Return results as a string (JSON preferred)</li>
     *   <li>Handle errors and return error messages</li>
     * </ul>
     *
     * @param context agent context with user, role, org, and limits
     * @param parameters tool parameters from AI
     * @return execution result (JSON string preferred)
     * @throws ToolExecutionException if execution fails
     */
    String execute(AgentContext context, Map<String, Object> parameters)
        throws ToolExecutionException;

    /**
     * Check if this tool requires specific permissions
     *
     * <p>Tools can define required permissions that will be checked
     * before execution. If the user's role doesn't have the required
     * permission, the tool will not be executed.
     *
     * @return required permission type, or null if no specific permission needed
     */
    default ToolPermission getRequiredPermission() {
        return null;
    }

    /**
     * Check if this tool is read-only
     *
     * <p>Read-only tools only query data and do not modify anything.
     * This information can be used for safety checks and audit logging.
     *
     * @return true if tool is read-only
     */
    default boolean isReadOnly() {
        return true;
    }

    /**
     * Get estimated cost of executing this tool (in tokens)
     *
     * <p>Used for cost estimation and budget enforcement.
     * This is a rough estimate of output tokens the tool might generate.
     *
     * @return estimated output tokens
     */
    default int getEstimatedOutputTokens() {
        return 500;
    }

    /**
     * Validate parameters before execution
     *
     * <p>Called before execute() to validate all required parameters
     * are present and valid. Throws exception if validation fails.
     *
     * @param parameters parameters to validate
     * @throws IllegalArgumentException if validation fails
     */
    default void validateParameters(Map<String, Object> parameters) {
        for (Map.Entry<String, ToolParameter> entry : getParameters().entrySet()) {
            String paramName = entry.getKey();
            ToolParameter paramDef = entry.getValue();

            if (paramDef.isRequired() && !parameters.containsKey(paramName)) {
                throw new IllegalArgumentException(
                    "Required parameter missing: " + paramName
                );
            }
        }
    }
}
