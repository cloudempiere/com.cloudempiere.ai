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
package com.cloudempiere.ai.context;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Container for component-specific parameters when extracting context.
 *
 * <p>This class provides a flexible, type-safe way to pass parameters
 * to context providers. It supports a fluent API for building parameter
 * sets and includes convenience methods for common parameter patterns.
 *
 * <p>Example usage:
 * <pre>
 * ContextParameters params = ContextParameters.forChart(chartId, sql)
 *     .put("chartModel", jsonModel)
 *     .put("chartJS", jsDefinition);
 *
 * JSONObject context = provider.extractContext(ctx, windowNo, params);
 * </pre>
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class ContextParameters {

    private final Map<String, Object> parameters;

    /**
     * Create empty parameter set
     */
    public ContextParameters() {
        this.parameters = new HashMap<>();
    }

    /**
     * Create parameter set with initial capacity
     *
     * @param initialCapacity expected number of parameters
     */
    public ContextParameters(int initialCapacity) {
        this.parameters = new HashMap<>(initialCapacity);
    }

    /**
     * Add a parameter to the set
     *
     * @param key parameter name
     * @param value parameter value (can be null)
     * @return this instance for fluent API
     */
    public ContextParameters put(String key, Object value) {
        parameters.put(key, value);
        return this;
    }

    /**
     * Get a parameter value
     *
     * @param key parameter name
     * @return parameter value, or null if not present
     */
    public Object get(String key) {
        return parameters.get(key);
    }

    /**
     * Get a parameter value with type casting
     *
     * @param <T> expected type
     * @param key parameter name
     * @param type expected class
     * @return parameter value cast to type, or null if not present
     * @throws ClassCastException if value cannot be cast to type
     */
    public <T> T get(String key, Class<T> type) {
        Object value = parameters.get(key);
        return value == null ? null : type.cast(value);
    }

    /**
     * Get a parameter value with default fallback
     *
     * @param <T> expected type
     * @param key parameter name
     * @param defaultValue value to return if parameter not present
     * @return parameter value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String key, T defaultValue) {
        Object value = parameters.get(key);
        return value == null ? defaultValue : (T) value;
    }

    /**
     * Get a string parameter value
     *
     * @param key parameter name
     * @return string value, or null if not present or not a string
     */
    public String getString(String key) {
        return get(key, String.class);
    }

    /**
     * Get an integer parameter value
     *
     * @param key parameter name
     * @return integer value, or null if not present or not an integer
     */
    public Integer getInt(String key) {
        return get(key, Integer.class);
    }

    /**
     * Get an integer parameter value with default
     *
     * @param key parameter name
     * @param defaultValue default value if not present
     * @return integer value or default
     */
    public int getInt(String key, int defaultValue) {
        Integer value = getInt(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Get a boolean parameter value
     *
     * @param key parameter name
     * @return boolean value, or null if not present or not a boolean
     */
    public Boolean getBoolean(String key) {
        return get(key, Boolean.class);
    }

    /**
     * Get a boolean parameter value with default
     *
     * @param key parameter name
     * @param defaultValue default value if not present
     * @return boolean value or default
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Boolean value = getBoolean(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Check if a parameter exists
     *
     * @param key parameter name
     * @return true if parameter is present (even if value is null)
     */
    public boolean has(String key) {
        return parameters.containsKey(key);
    }

    /**
     * Remove a parameter
     *
     * @param key parameter name
     * @return previous value, or null if not present
     */
    public Object remove(String key) {
        return parameters.remove(key);
    }

    /**
     * Get all parameter keys
     *
     * @return set of parameter names
     */
    public Set<String> keySet() {
        return parameters.keySet();
    }

    /**
     * Get number of parameters
     *
     * @return parameter count
     */
    public int size() {
        return parameters.size();
    }

    /**
     * Check if parameters are empty
     *
     * @return true if no parameters present
     */
    public boolean isEmpty() {
        return parameters.isEmpty();
    }

    /**
     * Clear all parameters
     */
    public void clear() {
        parameters.clear();
    }

    // ========================================================================
    // Builder methods for common parameter patterns
    // ========================================================================

    /**
     * Create parameters for window context extraction
     *
     * @param windowNo window number
     * @param tabNo tab number
     * @return parameter set with window/tab identifiers
     */
    public static ContextParameters forWindow(int windowNo, int tabNo) {
        return new ContextParameters(4)
            .put("windowNo", windowNo)
            .put("tabNo", tabNo);
    }

    /**
     * Create parameters for window context with record ID
     *
     * @param windowNo window number
     * @param tabNo tab number
     * @param recordId current record ID
     * @return parameter set with window/tab/record identifiers
     */
    public static ContextParameters forWindow(int windowNo, int tabNo, int recordId) {
        return forWindow(windowNo, tabNo)
            .put("recordId", recordId);
    }

    /**
     * Create parameters for chart context extraction
     *
     * @param chartId chart ID
     * @param sql SQL query that generates chart data
     * @return parameter set with chart identifiers
     */
    public static ContextParameters forChart(int chartId, String sql) {
        return new ContextParameters(4)
            .put("chartId", chartId)
            .put("sql", sql);
    }

    /**
     * Create parameters for chart context with model and JS
     *
     * @param chartId chart ID
     * @param sql SQL query
     * @param chartModel JSON chart model
     * @param chartJS JavaScript chart definition
     * @return parameter set with complete chart information
     */
    public static ContextParameters forChart(
        int chartId,
        String sql,
        String chartModel,
        String chartJS
    ) {
        return forChart(chartId, sql)
            .put("chartModel", chartModel)
            .put("chartJS", chartJS);
    }

    /**
     * Create parameters for process context extraction
     *
     * @param processId process/report ID
     * @return parameter set with process identifier
     */
    public static ContextParameters forProcess(int processId) {
        return new ContextParameters(2)
            .put("processId", processId);
    }

    /**
     * Create parameters for process with instance
     *
     * @param processId process/report ID
     * @param processInstanceId instance ID
     * @return parameter set with process and instance identifiers
     */
    public static ContextParameters forProcess(int processId, int processInstanceId) {
        return forProcess(processId)
            .put("processInstanceId", processInstanceId);
    }

    /**
     * Create parameters for dashboard context extraction
     *
     * @param dashboardId dashboard ID
     * @return parameter set with dashboard identifier
     */
    public static ContextParameters forDashboard(int dashboardId) {
        return new ContextParameters(2)
            .put("dashboardId", dashboardId);
    }

    /**
     * Create parameters for info window context extraction
     *
     * @param infoWindowId info window ID
     * @return parameter set with info window identifier
     */
    public static ContextParameters forInfoWindow(int infoWindowId) {
        return new ContextParameters(2)
            .put("infoWindowId", infoWindowId);
    }

    /**
     * Create parameters for knowledge base context extraction
     *
     * @param kType knowledge base type
     * @return parameter set with knowledge base type
     */
    public static ContextParameters forKnowledgeBase(String kType) {
        return new ContextParameters(2)
            .put("k_type", kType);
    }

    @Override
    public String toString() {
        return "ContextParameters" + parameters;
    }
}