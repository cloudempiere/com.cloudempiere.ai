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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.compiere.util.CLogger;

import com.cloudempiere.ai.provider.dto.AIFunction;

/**
 * Registry for AI Agent Tools
 *
 * <p>Manages registration, lookup, and conversion of tools for use
 * with AI agents. This registry provides:
 * <ul>
 *   <li>Tool registration and lookup by name</li>
 *   <li>Conversion to AIFunction DTOs for provider layer</li>
 *   <li>Tool filtering by permission and capability</li>
 *   <li>Thread-safe access</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>
 * ToolRegistry registry = new ToolRegistry();
 * registry.register(new DatabaseQueryTool());
 * registry.register(new FieldMetadataTool());
 *
 * ITool tool = registry.get("query_database");
 * List&lt;AIFunction&gt; functions = registry.toAIFunctions();
 * </pre>
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class ToolRegistry {

    private static final CLogger log = CLogger.getCLogger(ToolRegistry.class);

    /** Registered tools by name */
    private final Map<String, ITool> tools = new ConcurrentHashMap<>();

    /**
     * Create an empty tool registry
     */
    public ToolRegistry() {
    }

    /**
     * Register a tool
     *
     * <p>If a tool with the same name already exists, it will be replaced.
     *
     * @param tool tool to register
     * @throws IllegalArgumentException if tool is null or has invalid name
     */
    public void register(ITool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Tool cannot be null");
        }

        String name = tool.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tool name cannot be null or empty");
        }

        tools.put(name, tool);
        log.fine("Registered tool: " + name + " (" + tool.getClass().getSimpleName() + ")");
    }

    /**
     * Unregister a tool by name
     *
     * @param name tool name to unregister
     * @return the removed tool, or null if not found
     */
    public ITool unregister(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        ITool removed = tools.remove(name);
        if (removed != null) {
            log.fine("Unregistered tool: " + name);
        }
        return removed;
    }

    /**
     * Get a tool by name
     *
     * @param name tool name
     * @return the tool, or null if not found
     */
    public ITool get(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        return tools.get(name);
    }

    /**
     * Check if a tool is registered
     *
     * @param name tool name
     * @return true if tool is registered
     */
    public boolean has(String name) {
        return name != null && tools.containsKey(name);
    }

    /**
     * Get all registered tool names
     *
     * @return unmodifiable collection of tool names
     */
    public Collection<String> getToolNames() {
        return Collections.unmodifiableSet(tools.keySet());
    }

    /**
     * Get all registered tools
     *
     * @return unmodifiable collection of tools
     */
    public Collection<ITool> getTools() {
        return Collections.unmodifiableCollection(tools.values());
    }

    /**
     * Get number of registered tools
     *
     * @return tool count
     */
    public int size() {
        return tools.size();
    }

    /**
     * Check if registry is empty
     *
     * @return true if no tools registered
     */
    public boolean isEmpty() {
        return tools.isEmpty();
    }

    /**
     * Clear all registered tools
     */
    public void clear() {
        tools.clear();
        log.fine("Tool registry cleared");
    }

    /**
     * Get tools filtered by permission
     *
     * @param permission required permission (null returns all tools)
     * @return list of tools with matching permission
     */
    public List<ITool> getToolsByPermission(ToolPermission permission) {
        if (permission == null) {
            return new ArrayList<>(tools.values());
        }

        List<ITool> filtered = new ArrayList<>();
        for (ITool tool : tools.values()) {
            if (permission.equals(tool.getRequiredPermission())) {
                filtered.add(tool);
            }
        }
        return filtered;
    }

    /**
     * Get only read-only tools
     *
     * @return list of read-only tools
     */
    public List<ITool> getReadOnlyTools() {
        List<ITool> filtered = new ArrayList<>();
        for (ITool tool : tools.values()) {
            if (tool.isReadOnly()) {
                filtered.add(tool);
            }
        }
        return filtered;
    }

    /**
     * Convert all registered tools to AIFunction DTOs
     *
     * <p>This method converts the tool definitions to the format
     * expected by the provider layer (IAIProvider.generateTextWithFunctions).
     *
     * @return list of AIFunction objects
     */
    public List<AIFunction> toAIFunctions() {
        List<AIFunction> functions = new ArrayList<>();

        for (ITool tool : tools.values()) {
            functions.add(toAIFunction(tool));
        }

        return functions;
    }

    /**
     * Convert a single tool to AIFunction DTO
     *
     * @param tool tool to convert
     * @return AIFunction object
     */
    public AIFunction toAIFunction(ITool tool) {
        AIFunction function = new AIFunction();
        function.setName(tool.getName());
        function.setDescription(tool.getDescription());

        // Convert parameters to JSON schema format
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("type", "object");

        Map<String, Object> properties = new HashMap<>();
        List<String> required = new ArrayList<>();

        for (Map.Entry<String, ToolParameter> entry : tool.getParameters().entrySet()) {
            String paramName = entry.getKey();
            ToolParameter paramDef = entry.getValue();

            Map<String, Object> paramSchema = new HashMap<>();
            paramSchema.put("type", paramDef.getJsonType());
            paramSchema.put("description", paramDef.getDescription());

            if (paramDef.getDefaultValue() != null) {
                paramSchema.put("default", paramDef.getDefaultValue());
            }

            properties.put(paramName, paramSchema);

            if (paramDef.isRequired()) {
                required.add(paramName);
            }
        }

        parameters.put("properties", properties);
        if (!required.isEmpty()) {
            parameters.put("required", required);
        }

        function.setParameters(parameters);

        return function;
    }

    /**
     * Create a registry with default ERP tools
     *
     * <p>Factory method to create a registry pre-populated with
     * standard iDempiere tools.
     *
     * @return new ToolRegistry with default tools
     */
    public static ToolRegistry createDefault() {
        ToolRegistry registry = new ToolRegistry();

        // Register default tools
        // TODO: Add default tool registrations as they are implemented
        // registry.register(new DatabaseQueryTool());
        // registry.register(new FieldMetadataTool());
        // registry.register(new TableMetadataTool());

        log.info("Created default tool registry with " + registry.size() + " tools");
        return registry;
    }

    @Override
    public String toString() {
        return "ToolRegistry{" +
               "tools=" + tools.keySet() +
               '}';
    }
}
