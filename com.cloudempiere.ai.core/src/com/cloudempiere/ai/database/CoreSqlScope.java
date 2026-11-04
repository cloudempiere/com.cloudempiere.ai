package com.cloudempiere.ai.database;

import org.compiere.model.AccessSqlParser;

/**
 * Which AI-written SQL core can secure (CLD-2546). Only core decides: {@link AccessSqlParser} reads the statement, and
 * {@code MRole.addAccessSQL} secures it.
 *
 * <p>Core adds the tenant and org filter to the first table of the main SELECT only, and nothing to a subquery: core
 * never runs user-written SQL, its own joins follow foreign keys inside the tenant. So the AI may run one SELECT on one
 * table, the case core secures completely. Related records are a second query (one more table, again secured by core).
 *
 * <p>No OSGi or database dependency, so it can be unit-tested.
 */
public final class CoreSqlScope {

    private CoreSqlScope() {
    }

    /**
     * @param sql the statement without ORDER BY / GROUP BY / HAVING / LIMIT (as {@code addAccessSQL} gets it)
     * @throws SecurityException when the statement has a subquery or more than one table; the message tells the model
     *         what to do instead
     */
    public static void assertSecurableByCore(String sql) {
        AccessSqlParser parser = new AccessSqlParser(sql);
        if (parser.getNoSqlStatments() != 1) {
            throw new SecurityException("Subqueries are not allowed: query one table at a time and look up related "
                + "records with a second query");
        }
        AccessSqlParser.TableInfo[] tables = parser.getTableInfo(parser.getMainSqlIndex());
        if (tables == null || tables.length == 0) {
            // core's parser reads the keywords in upper case only (" FROM ", " WHERE ")
            throw new SecurityException("No table found: write SQL keywords in upper case "
                + "(SELECT ... FROM ... WHERE ...)");
        }
        if (tables.length != 1) {
            throw new SecurityException("Only one table per query: look up related records with a second query "
                + "(e.g. lookupRecord)");
        }
    }
}
