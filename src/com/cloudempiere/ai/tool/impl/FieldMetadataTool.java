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
package com.cloudempiere.ai.tool.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cloudempiere.ai.agent.AgentContext;
import com.cloudempiere.ai.tool.ITool;
import com.cloudempiere.ai.tool.ToolExecutionException;
import com.cloudempiere.ai.tool.ToolParameter;
import com.cloudempiere.ai.tool.ToolPermission;

/**
 * Tool for retrieving field metadata from Application Dictionary
 *
 * <p>Provides information about fields in iDempiere tables, including:
 * <ul>
 *   <li>Column name and type</li>
 *   <li>Display name and description</li>
 *   <li>Reference type (list, table, etc.)</li>
 *   <li>Validation rules</li>
 *   <li>Default values</li>
 * </ul>
 *
 * <p>This helps the AI understand the data model and write better queries.
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class FieldMetadataTool implements ITool {

    private static final CLogger log = CLogger.getCLogger(FieldMetadataTool.class);

    public static final String TOOL_NAME = "get_field_metadata";

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return "Get metadata about fields/columns in an iDempiere table. " +
               "Returns column names, data types, descriptions, and validation rules. " +
               "Use this to understand the data model before writing queries.";
    }

    @Override
    public Map<String, ToolParameter> getParameters() {
        Map<String, ToolParameter> params = new LinkedHashMap<>();

        params.put("table_name", ToolParameter.requiredString(
            "The name of the table to get field metadata for (e.g., 'M_Product', 'C_Order')"
        ));

        params.put("column_name", ToolParameter.optionalString(
            "Optional: specific column name to get details for. If omitted, returns all columns.",
            null
        ));

        return params;
    }

    @Override
    public String execute(AgentContext context, Map<String, Object> parameters)
            throws ToolExecutionException {

        validateParameters(parameters);

        String tableName = (String) parameters.get("table_name");
        String columnName = (String) parameters.get("column_name");

        log.fine("Getting field metadata for table: " + tableName +
                (columnName != null ? ", column: " + columnName : ""));

        try {
            JSONObject result = new JSONObject();
            result.put("table_name", tableName);

            // Get table info
            int tableId = getTableId(tableName);
            if (tableId <= 0) {
                throw ToolExecutionException.notFound("Table not found: " + tableName);
            }

            result.put("table_id", tableId);

            // Get columns
            JSONArray columns = getColumnMetadata(tableId, columnName);
            result.put("columns", columns);
            result.put("column_count", columns.length());

            return result.toString();

        } catch (ToolExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.severe("Error getting field metadata: " + e.getMessage());
            throw new ToolExecutionException("Failed to get field metadata: " + e.getMessage(), e);
        }
    }

    /**
     * Get table ID from table name
     */
    private int getTableId(String tableName) {
        return DB.getSQLValue(null,
            "SELECT AD_Table_ID FROM AD_Table WHERE UPPER(TableName)=?",
            tableName.toUpperCase());
    }

    /**
     * Get column metadata for a table
     */
    private JSONArray getColumnMetadata(int tableId, String columnName) {
        JSONArray columns = new JSONArray();

        String sql = "SELECT " +
            "c.ColumnName, " +
            "c.Name, " +
            "c.Description, " +
            "c.Help, " +
            "c.AD_Reference_ID, " +
            "r.Name AS ReferenceType, " +
            "c.AD_Reference_Value_ID, " +
            "c.FieldLength, " +
            "c.IsMandatory, " +
            "c.IsKey, " +
            "c.IsIdentifier, " +
            "c.DefaultValue, " +
            "c.ValueMin, " +
            "c.ValueMax, " +
            "c.IsEncrypted, " +
            "c.IsActive " +
            "FROM AD_Column c " +
            "LEFT JOIN AD_Reference r ON c.AD_Reference_ID = r.AD_Reference_ID " +
            "WHERE c.AD_Table_ID = ? " +
            "AND c.IsActive = 'Y' ";

        if (columnName != null && !columnName.trim().isEmpty()) {
            sql += "AND UPPER(c.ColumnName) = ? ";
        }

        sql += "ORDER BY c.SeqNo, c.ColumnName";

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = DB.prepareStatement(sql, null);
            pstmt.setInt(1, tableId);
            if (columnName != null && !columnName.trim().isEmpty()) {
                pstmt.setString(2, columnName.toUpperCase());
            }

            rs = pstmt.executeQuery();

            while (rs.next()) {
                JSONObject col = new JSONObject();
                col.put("column_name", rs.getString("ColumnName"));
                col.put("display_name", rs.getString("Name"));
                col.put("description", rs.getString("Description"));
                col.put("help", rs.getString("Help"));
                col.put("reference_id", rs.getInt("AD_Reference_ID"));
                col.put("reference_type", rs.getString("ReferenceType"));
                col.put("field_length", rs.getInt("FieldLength"));
                col.put("is_mandatory", "Y".equals(rs.getString("IsMandatory")));
                col.put("is_key", "Y".equals(rs.getString("IsKey")));
                col.put("is_identifier", "Y".equals(rs.getString("IsIdentifier")));
                col.put("default_value", rs.getString("DefaultValue"));
                col.put("value_min", rs.getString("ValueMin"));
                col.put("value_max", rs.getString("ValueMax"));
                col.put("is_encrypted", "Y".equals(rs.getString("IsEncrypted")));

                // Add reference values if it's a list
                int refValueId = rs.getInt("AD_Reference_Value_ID");
                if (refValueId > 0) {
                    col.put("reference_values", getReferenceValues(refValueId));
                }

                columns.put(col);
            }

        } catch (Exception e) {
            log.severe("Error querying column metadata: " + e.getMessage());
        } finally {
            DB.close(rs, pstmt);
        }

        return columns;
    }

    /**
     * Get reference list values
     */
    private JSONArray getReferenceValues(int referenceValueId) {
        JSONArray values = new JSONArray();

        String sql = "SELECT Value, Name, Description " +
                    "FROM AD_Ref_List " +
                    "WHERE AD_Reference_ID = ? " +
                    "AND IsActive = 'Y' " +
                    "ORDER BY Value";

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = DB.prepareStatement(sql, null);
            pstmt.setInt(1, referenceValueId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                JSONObject val = new JSONObject();
                val.put("value", rs.getString("Value"));
                val.put("name", rs.getString("Name"));
                val.put("description", rs.getString("Description"));
                values.put(val);
            }

        } catch (Exception e) {
            log.warning("Error getting reference values: " + e.getMessage());
        } finally {
            DB.close(rs, pstmt);
        }

        return values;
    }

    @Override
    public ToolPermission getRequiredPermission() {
        return ToolPermission.READ_METADATA;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public int getEstimatedOutputTokens() {
        return 500;
    }
}
