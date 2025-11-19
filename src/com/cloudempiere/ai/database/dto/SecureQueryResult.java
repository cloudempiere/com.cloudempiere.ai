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
package com.cloudempiere.ai.database.dto;

import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Result of secure database query execution
 *
 * <p>Contains query results, metadata, and execution statistics.
 *
 * @author Cloudempiere
 * @version 2.0
 */
public class SecureQueryResult {

    /** Status: SUCCESS, PERMISSION_DENIED, ERROR */
    private String status;

    /** Error message if status is not SUCCESS */
    private String errorMessage;

    /** User ID (logged-in user) */
    private int userId;

    /** Role ID used for permissions */
    private int roleId;

    /** Role name */
    private String roleName;

    /** SQL after security injection */
    private String securedSQL;

    /** List of tables accessed */
    private List<String> tablesAccessed;

    /** Column names */
    private JSONArray columns;

    /** Column types */
    private JSONArray columnTypes;

    /** Result rows */
    private JSONArray rows;

    /** Number of rows returned */
    private int rowCount;

    /** Query execution time in milliseconds */
    private long queryExecutionTimeMs;

    /** Total execution time in milliseconds */
    private long totalExecutionTimeMs;

    /**
     * Get status
     * @return Status (SUCCESS, PERMISSION_DENIED, ERROR)
     */
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Get error message
     * @return Error message or null if successful
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Get user ID
     * @return AD_User_ID of logged-in user
     */
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    /**
     * Get role ID
     * @return AD_Role_ID used for permissions
     */
    public int getRoleId() {
        return roleId;
    }

    public void setRoleId(int roleId) {
        this.roleId = roleId;
    }

    /**
     * Get role name
     * @return Role name
     */
    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    /**
     * Get secured SQL
     * @return SQL after MRole.addAccessSQL() injection
     */
    public String getSecuredSQL() {
        return securedSQL;
    }

    public void setSecuredSQL(String securedSQL) {
        this.securedSQL = securedSQL;
    }

    /**
     * Get tables accessed
     * @return List of table names
     */
    public List<String> getTablesAccessed() {
        return tablesAccessed;
    }

    public void setTablesAccessed(List<String> tablesAccessed) {
        this.tablesAccessed = tablesAccessed;
    }

    /**
     * Get column names
     * @return JSONArray of column names
     */
    public JSONArray getColumns() {
        return columns;
    }

    public void setColumns(JSONArray columns) {
        this.columns = columns;
    }

    /**
     * Get column types
     * @return JSONArray of column types
     */
    public JSONArray getColumnTypes() {
        return columnTypes;
    }

    public void setColumnTypes(JSONArray columnTypes) {
        this.columnTypes = columnTypes;
    }

    /**
     * Get result rows
     * @return JSONArray of row objects
     */
    public JSONArray getRows() {
        return rows;
    }

    public void setRows(JSONArray rows) {
        this.rows = rows;
    }

    /**
     * Get row count
     * @return Number of rows returned
     */
    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    /**
     * Get query execution time
     * @return Query execution time in milliseconds
     */
    public long getQueryExecutionTimeMs() {
        return queryExecutionTimeMs;
    }

    public void setQueryExecutionTimeMs(long queryExecutionTimeMs) {
        this.queryExecutionTimeMs = queryExecutionTimeMs;
    }

    /**
     * Get total execution time
     * @return Total execution time in milliseconds
     */
    public long getTotalExecutionTimeMs() {
        return totalExecutionTimeMs;
    }

    public void setTotalExecutionTimeMs(long totalExecutionTimeMs) {
        this.totalExecutionTimeMs = totalExecutionTimeMs;
    }

    /**
     * Check if query was successful
     * @return true if status is SUCCESS
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }

    /**
     * Convert result to JSON
     * @return JSON representation of the result
     */
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("status", status);
        json.put("user_id", userId);
        json.put("role_id", roleId);
        json.put("role_name", roleName);
        json.put("tables_accessed", tablesAccessed);
        json.put("columns", columns);
        json.put("column_types", columnTypes);
        json.put("rows", rows);
        json.put("row_count", rowCount);
        json.put("query_execution_time_ms", queryExecutionTimeMs);
        json.put("total_execution_time_ms", totalExecutionTimeMs);

        if (errorMessage != null) {
            json.put("error", errorMessage);
        }

        return json;
    }

    @Override
    public String toString() {
        return toJSON().toString(2);
    }
}
