/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2025 Cloudempiere                                            *
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
package com.cloudempiere.ai.provider.langchain4j;

import org.compiere.util.DB;

/**
 * Helper class for generating database-specific SQL syntax guidance for AI agents.
 *
 * <p>This class detects the database type (PostgreSQL or Oracle) and provides
 * appropriate SQL syntax rules to prevent common syntax errors like using
 * YEAR() function on PostgreSQL (which doesn't exist).
 *
 * @author Cloudempiere AI Team
 * @version 0.20.0
 * @since Database type detection for AI queries
 */
public class DatabaseSyntaxHelper {

    /**
     * Get database-specific SQL syntax guidance based on detected database type.
     *
     * @return SQL syntax rules for the current database (PostgreSQL or Oracle)
     */
    public static String getDatabaseSyntaxGuidance() {
        if (DB.isPostgreSQL()) {
            // Note: PostgreSQL version-specific features could be detected via:
            // SELECT version(); -- Returns: PostgreSQL 12.x, 13.x, 14.x, 15.x, etc.
            // Most modern PostgreSQL (10+) supports EXTRACT, LIMIT, etc.
            return "\n\nCRITICAL SQL SYNTAX RULES (PostgreSQL Database):\n" +
                   "The database is PostgreSQL. You MUST use PostgreSQL syntax, NOT Oracle/MySQL syntax:\n" +
                   "✓ CORRECT PostgreSQL syntax:\n" +
                   "  - Date/Time functions: EXTRACT(YEAR FROM date_column), EXTRACT(MONTH FROM date_column), EXTRACT(DAY FROM date_column)\n" +
                   "  - String concat: column1 || ' ' || column2 OR CONCAT(column1, ' ', column2)\n" +
                   "  - Case-insensitive search: ILIKE 'pattern%'\n" +
                   "  - Current date: CURRENT_DATE or NOW()\n" +
                   "  - Date arithmetic: date_column + INTERVAL '1 day'\n" +
                   "  - Limit rows: LIMIT n OFFSET m\n" +
                   "  - Date truncation: DATE_TRUNC('month', date_column)\n" +
                   "✗ WRONG syntax (will cause errors):\n" +
                   "  - YEAR(date_column) - does NOT exist in PostgreSQL! Use EXTRACT(YEAR FROM date_column)\n" +
                   "  - MONTH(date_column) - does NOT exist in PostgreSQL! Use EXTRACT(MONTH FROM date_column)\n" +
                   "  - DAY(date_column) - does NOT exist in PostgreSQL! Use EXTRACT(DAY FROM date_column)\n" +
                   "  - DATEPART() - Oracle/SQL Server syntax, not PostgreSQL\n" +
                   "  - ROWNUM - Oracle syntax, use LIMIT instead\n" +
                   "  - TOP n - SQL Server syntax, use LIMIT instead\n" +
                   "Always use EXTRACT(field FROM column) for date parts in PostgreSQL.";
        } else if (DB.isOracle()) {
            // Note: Oracle version-specific features could be detected via:
            // SELECT * FROM V$VERSION; -- Returns: Oracle Database 11g, 12c, 18c, 19c, 21c, etc.
            // Oracle 12c+ supports FETCH FIRST, older versions use ROWNUM
            return "\n\nCRITICAL SQL SYNTAX RULES (Oracle Database):\n" +
                   "The database is Oracle. You MUST use Oracle syntax:\n" +
                   "✓ CORRECT Oracle syntax:\n" +
                   "  - Date/Time functions: EXTRACT(YEAR FROM date_column), EXTRACT(MONTH FROM date_column)\n" +
                   "  - String concat: column1 || ' ' || column2 OR CONCAT(column1, column2)\n" +
                   "  - Case-insensitive search: UPPER(column) LIKE UPPER('pattern%')\n" +
                   "  - Current date: SYSDATE or CURRENT_DATE\n" +
                   "  - Date arithmetic: date_column + 1 (adds 1 day)\n" +
                   "  - Limit rows: FETCH FIRST n ROWS ONLY (Oracle 12c+) or use ROWNUM\n" +
                   "  - Date truncation: TRUNC(date_column, 'MM')\n" +
                   "✗ WRONG syntax (will cause errors):\n" +
                   "  - LIMIT n - PostgreSQL syntax, not Oracle\n" +
                   "  - ILIKE - PostgreSQL syntax, not Oracle\n" +
                   "  - INTERVAL '1 day' - PostgreSQL syntax, use +1 instead\n" +
                   "  - NOW() - PostgreSQL syntax, use SYSDATE instead";
        } else {
            // Generic SQL guidance for unknown database
            return "\n\nSQL SYNTAX RULES:\n" +
                   "Use standard SQL syntax. For date functions, prefer EXTRACT(field FROM column) syntax.";
        }
    }

    /**
     * Get complete system prompt with database-specific SQL syntax guidance.
     *
     * @param basePrompt The base system prompt to append database guidance to
     * @return Complete system prompt with database syntax guidance
     */
    public static String appendDatabaseGuidance(String basePrompt) {
        return basePrompt + getDatabaseSyntaxGuidance();
    }
}
