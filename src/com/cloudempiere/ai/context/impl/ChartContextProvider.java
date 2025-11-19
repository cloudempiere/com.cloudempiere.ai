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
package com.cloudempiere.ai.context.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Properties;
import java.util.logging.Level;

import org.adempiere.apps.graph.model.ChartDatasource;
import org.compiere.model.MChart;
import org.compiere.model.MChartPara;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.json.JSONArray;
import org.json.JSONObject;

import com.cloudempiere.ai.context.ContextParameters;
import com.cloudempiere.ai.context.IAIContextProvider;

/**
 * Context provider for Billboard.js dashboard charts
 *
 * <p>Extracts comprehensive context about a chart including:
 * <ul>
 *   <li>Chart metadata (name, type, period)</li>
 *   <li>SQL source query</li>
 *   <li>Query results (actual data)</li>
 *   <li>JSON chart model (rendering config)</li>
 *   <li>JavaScript chart definition</li>
 *   <li>User filter parameters</li>
 *   <li>Dashboard context</li>
 * </ul>
 *
 * <p>This context enables AI to:
 * <ul>
 *   <li>Explain what the chart shows</li>
 *   <li>Analyze trends and patterns</li>
 *   <li>Answer questions about the data</li>
 *   <li>Suggest insights or actions</li>
 * </ul>
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class ChartContextProvider implements IAIContextProvider {

    private static final CLogger log = CLogger.getCLogger(ChartContextProvider.class);

    /** Maximum rows to include in context */
    private static final int MAX_ROWS = 100;

    @Override
    public String getContextType() {
        return "CHART";
    }

    @Override
    public JSONObject extractContext(
        Properties ctx,
        int windowNo,
        ContextParameters parameters
    ) {
        JSONObject context = new JSONObject();

        try {
            // 1. Extract basic user context
            addUserContext(context, ctx);

            // 2. Extract chart metadata
            Integer chartId = parameters.getInt("chartId");
            if (chartId == null) {
                throw new IllegalArgumentException("chartId parameter is required");
            }

            MChart chart = MChart.get(ctx, chartId);
            if (chart == null) {
                throw new IllegalArgumentException("Chart not found: " + chartId);
            }

            addChartMetadata(context, chart, ctx);

            // 3. Extract SQL query with context variable substitution
            String sql = parameters.getString("sql");
            if (sql != null && sql.length() > 0) {
                String parsedSQL = Env.parseContext(ctx, windowNo, sql, false);
                context.put("sql_query", parsedSQL);

                // 4. Execute query and capture results
                if (parsedSQL != null && parsedSQL.length() > 0) {
                    addQueryResults(context, ctx, parsedSQL, MAX_ROWS);
                }
            }

            // 5. Extract chart model (JSON configuration)
            String chartModel = parameters.getString("chartModel");
            if (chartModel != null && chartModel.length() > 0) {
                try {
                    context.put("chart_model", new JSONObject(chartModel));
                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed to parse chart model JSON", e);
                    context.put("chart_model_raw", chartModel);
                }
            }

            // 6. Extract JavaScript chart definition
            String chartJS = parameters.getString("chartJS");
            if (chartJS != null && chartJS.length() > 0) {
                context.put("chart_javascript", chartJS);
            }

            // 7. Extract chart parameters (user filters)
            addChartParameters(context, chart, ctx, windowNo);

            // 8. Extract datasource information
            addDatasourceInfo(context, chart);

            // 9. Add dashboard context if available
            Integer dashboardId = parameters.getInt("dashboardId");
            if (dashboardId != null) {
                addDashboardContext(context, dashboardId, ctx);
            }

            // 10. Add rendering metadata
            addRenderingMetadata(context, parameters);

            context.put("success", true);

        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed to extract chart context", e);
            context.put("error", "Failed to extract context: " + e.getMessage());
            context.put("success", false);
        }

        return context;
    }

    /**
     * Add user context (who is viewing this chart)
     */
    private void addUserContext(JSONObject context, Properties ctx) {
        JSONObject userContext = new JSONObject();
        userContext.put("user_id", Env.getAD_User_ID(ctx));
        userContext.put("user_name", Env.getContext(ctx, "#AD_User_Name"));
        userContext.put("client_id", Env.getAD_Client_ID(ctx));
        userContext.put("client_name", Env.getContext(ctx, "#AD_Client_Name"));
        userContext.put("org_id", Env.getAD_Org_ID(ctx));
        userContext.put("org_name", Env.getContext(ctx, "#AD_Org_Name"));
        userContext.put("role_id", Env.getAD_Role_ID(ctx));
        userContext.put("role_name", Env.getContext(ctx, "#AD_Role_Name"));
        userContext.put("language", Env.getContext(ctx, "#AD_Language"));
        userContext.put("date", Env.getContext(ctx, "#Date"));

        context.put("user_context", userContext);
    }

    /**
     * Add chart metadata
     */
    private void addChartMetadata(
        JSONObject context,
        MChart chart,
        Properties ctx
    ) {
        JSONObject chartMeta = new JSONObject();
        chartMeta.put("chart_id", chart.getAD_Chart_ID());
        chartMeta.put("name", chart.getName());
        chartMeta.put("description", chart.getDescription());
        chartMeta.put("chart_type", chart.getChartType());
        chartMeta.put("time_unit", chart.getTimeUnit());
        chartMeta.put("time_scope", chart.getTimeScope());
        chartMeta.put("is_active", chart.isActive());

        // Add translated name/description if available
        String language = Env.getContext(ctx, "#AD_Language");
        if (language != null && !language.equals("en_US")) {
            String translatedName = chart.get_Translation("Name", language);
            if (translatedName != null && !translatedName.equals(chart.getName())) {
                chartMeta.put("name_translated", translatedName);
            }
            String translatedDesc = chart.get_Translation("Description", language);
            if (translatedDesc != null && !translatedDesc.equals(chart.getDescription())) {
                chartMeta.put("description_translated", translatedDesc);
            }
        }

        context.put("chart_metadata", chartMeta);
    }

    /**
     * Execute query and capture results
     */
    private void addQueryResults(
        JSONObject context,
        Properties ctx,
        String sql,
        int maxRows
    ) {
        JSONArray results = new JSONArray();
        JSONArray columnNames = new JSONArray();
        JSONArray columnTypes = new JSONArray();

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = DB.prepareStatement(sql, null);
            rs = pstmt.executeQuery();

            // Get column metadata
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                columnNames.put(rsmd.getColumnName(i));
                columnTypes.put(rsmd.getColumnTypeName(i));
            }

            // Get data rows (limited)
            int rowCount = 0;
            while (rs.next() && rowCount < maxRows) {
                JSONObject row = new JSONObject();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rsmd.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value != null ? value.toString() : null);
                }
                results.put(row);
                rowCount++;
            }

            // Check if there are more rows
            boolean hasMore = false;
            if (rowCount >= maxRows) {
                hasMore = rs.next();
            }

            JSONObject queryResults = new JSONObject();
            queryResults.put("columns", columnNames);
            queryResults.put("column_types", columnTypes);
            queryResults.put("rows", results);
            queryResults.put("row_count", rowCount);
            queryResults.put("truncated", hasMore);

            context.put("query_results", queryResults);

        } catch (Exception e) {
            log.log(Level.WARNING, "Failed to execute chart query", e);
            context.put("query_error", e.getMessage());
        } finally {
            DB.close(rs, pstmt);
        }
    }

    /**
     * Add chart parameters (filters, date ranges, etc.)
     */
    private void addChartParameters(
        JSONObject context,
        MChart chart,
        Properties ctx,
        int windowNo
    ) {
        JSONArray parameters = new JSONArray();

        MChartPara[] chartParas = chart.getParameters();
        if (chartParas != null && chartParas.length > 0) {
            for (MChartPara para : chartParas) {
                JSONObject paramObj = new JSONObject();
                paramObj.put("name", para.getName());
                paramObj.put("column_name", para.getColumnName());
                paramObj.put("reference_id", para.getAD_Reference_ID());

                // Get actual value from context
                String value = Env.getContext(ctx, windowNo, para.getColumnName());
                paramObj.put("value", value);

                parameters.put(paramObj);
            }
        }

        context.put("chart_parameters", parameters);
    }

    /**
     * Add datasource information
     */
    private void addDatasourceInfo(
        JSONObject context,
        MChart chart
    ) {
        JSONArray datasources = new JSONArray();

        ChartDatasource[] chartDatasources = chart.getDatasources();
        if (chartDatasources != null && chartDatasources.length > 0) {
            for (ChartDatasource ds : chartDatasources) {
                JSONObject dsObj = new JSONObject();
                dsObj.put("name", ds.getName());
                dsObj.put("description", ds.getDescription());
                dsObj.put("entity_type", ds.getEntityType());
                dsObj.put("from_clause", ds.getFromClause());
                dsObj.put("where_clause", ds.getWhereClause());
                dsObj.put("date_column", ds.getDateColumn());
                dsObj.put("value_column", ds.getValueColumn());
                dsObj.put("category_column", ds.getCategoryColumn());

                datasources.put(dsObj);
            }
        }

        context.put("datasources", datasources);
    }

    /**
     * Add dashboard context (which dashboard panel)
     */
    private void addDashboardContext(
        JSONObject context,
        int dashboardId,
        Properties ctx
    ) {
        JSONObject dashboardContext = new JSONObject();
        dashboardContext.put("dashboard_id", dashboardId);

        // Could fetch dashboard name, layout, other panels, etc.
        // For now, just the ID

        context.put("dashboard_context", dashboardContext);
    }

    /**
     * Add rendering metadata (width, height, color scheme)
     */
    private void addRenderingMetadata(
        JSONObject context,
        ContextParameters parameters
    ) {
        JSONObject rendering = new JSONObject();

        Integer width = parameters.getInt("chartWidth");
        if (width != null) {
            rendering.put("width", width);
        }

        Integer height = parameters.getInt("chartHeight");
        if (height != null) {
            rendering.put("height", height);
        }

        String colorScheme = parameters.getString("colorScheme");
        if (colorScheme != null) {
            rendering.put("color_scheme", colorScheme);
        }

        if (rendering.length() > 0) {
            context.put("rendering", rendering);
        }
    }

    @Override
    public boolean validateContext(JSONObject context) {
        // Validate required fields
        return context.has("chart_metadata") &&
               context.has("user_context") &&
               context.optBoolean("success", false);
    }

    @Override
    public String[] getSensitiveFields() {
        // Fields that should be redacted if present in chart data
        return new String[] {
            "Password",
            "UserPIN",
            "CreditCardNumber",
            "CreditCardVV",
            "CVV",
            "SSN",
            "TaxID",
            "BankAccount",
            "BankAccountNo",
            "IBAN",
            "APIKey",
            "AccessToken",
            "SecretKey"
        };
    }

    @Override
    public String getDescription() {
        return "Context provider for Billboard.js dashboard charts";
    }
}