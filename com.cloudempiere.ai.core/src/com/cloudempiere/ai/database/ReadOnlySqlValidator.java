package com.cloudempiere.ai.database;

import java.util.regex.Pattern;

/**
 * Text check for the SQL that an AI tool wants to run (CLD-2355).
 *
 * <p>This is the <b>second</b> layer. The first is that {@link SecureDatabaseQueryExecutor} runs the statement in a
 * read-only transaction, which the database enforces. A text check cannot prove that a statement is harmless, but it
 * rejects what a read-only transaction does not stop, such as functions that have side effects without writing a table
 * ({@code pg_terminate_backend}, {@code set_config}, {@code nextval}, ...), and it gives the model an early, readable
 * error for the obvious mistakes.
 *
 * <p>Rules, applied to the statement with the contents of its quoted literals and identifiers removed (so {@code Name LIKE '%set%'} is
 * fine):
 * <ul>
 *   <li>the statement starts with {@code SELECT}; there is no second statement and no comment;</li>
 *   <li>no data-changing or transaction-changing keyword as a whole word (including {@code INTO}, which turns a SELECT
 *       into a table-creating statement in PostgreSQL, and {@code SET}/{@code COMMIT} which change transaction
 *       semantics);</li>
 *   <li>no row locking clause ({@code FOR UPDATE}, {@code FOR SHARE}, ...);</li>
 *   <li>no call of a function from the deny list;</li>
 *   <li>no {@code $} or backslash, which allow quoting forms this check does not parse (dollar quoting, escape
 *       strings).</li>
 * </ul>
 * It has no dependency on iDempiere so it can be unit-tested on its own.
 */
public final class ReadOnlySqlValidator {

    private ReadOnlySqlValidator() {
    }

    /** Whole-word keywords that change data, schema, or the semantics of the transaction. */
    private static final String[] FORBIDDEN_KEYWORDS = {
        "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER",
        "TRUNCATE", "MERGE", "GRANT", "REVOKE", "EXECUTE",
        "CALL", "EXEC",
        // CLD-2355: SELECT ... INTO creates a table in PostgreSQL
        "INTO",
        // CLD-2355: bulk file/stdin transfer, and statements that end or reconfigure the transaction
        "COPY", "COMMIT", "ROLLBACK", "ABORT", "SET", "RESET"
    };

    private static final Pattern[] FORBIDDEN_KEYWORD_PATTERNS = compile(FORBIDDEN_KEYWORDS);

    /** Row locking clauses: they block other sessions even though they write nothing. */
    private static final Pattern LOCKING_CLAUSE =
        Pattern.compile("\\bFOR\\s+(NO\\s+KEY\\s+UPDATE|KEY\\s+SHARE|SHARE|UPDATE)\\b");

    /**
     * Functions that act on the server, the session, a sequence or the file system without being an INSERT/UPDATE, so
     * a read-only transaction does not stop them. Matched as a whole name followed by an opening parenthesis.
     */
    private static final Pattern DENIED_FUNCTION = Pattern.compile(
        "\\b(PG_TERMINATE_BACKEND|PG_CANCEL_BACKEND|PG_SLEEP\\w*|PG_RELOAD_CONF|PG_ROTATE_LOGFILE"
        + "|PG_READ_FILE|PG_READ_BINARY_FILE|PG_LS_DIR|PG_STAT_FILE|PG_ADVISORY\\w*|PG_TRY_ADVISORY\\w*"
        + "|SET_CONFIG|NEXTVAL|SETVAL|LO_\\w+|DBLINK\\w*|DBMS_[\\w.]+|UTL_[\\w.]+)\\s*\\(");

    private static Pattern[] compile(String[] words) {
        Pattern[] result = new Pattern[words.length];
        for (int i = 0; i < words.length; i++) {
            result[i] = Pattern.compile("\\b" + words[i] + "\\b");
        }
        return result;
    }

    /**
     * Check a statement.
     *
     * @param sql the statement
     * @throws IllegalArgumentException if the statement is null or blank
     * @throws SecurityException        if the statement is not an acceptable read-only SELECT
     */
    public static void validate(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL query is required");
        }

        // one blank between tokens, so "INSERT\tINTO" and "INSERT\nINTO" are seen like "INSERT INTO"
        String upper = sql.trim().replaceAll("\\s+", " ").toUpperCase();

        // Must start with SELECT
        if (!upper.startsWith("SELECT")) {
            throw new SecurityException("Only SELECT queries are allowed");
        }

        // What quoted literals and identifiers contain is data, not SQL: remove it before looking for keywords
        String code = stripQuoted(upper);

        for (int i = 0; i < FORBIDDEN_KEYWORDS.length; i++) {
            if (FORBIDDEN_KEYWORD_PATTERNS[i].matcher(code).find()) {
                throw new SecurityException("Forbidden SQL keyword: " + FORBIDDEN_KEYWORDS[i]);
            }
        }

        // No multiple statements (check if there's anything after semicolon besides whitespace)
        int semicolonIndex = code.indexOf(";");
        if (semicolonIndex >= 0) {
            String afterSemicolon = code.substring(semicolonIndex + 1).trim();
            if (!afterSemicolon.isEmpty()) {
                throw new SecurityException("Multiple SQL statements not allowed");
            }
        }

        // No comments that could hide malicious code
        if (sql.contains("--") || sql.contains("/*")) {
            throw new SecurityException("SQL comments not allowed");
        }

        // Quoting forms this check does not parse
        if (sql.indexOf('$') >= 0 || sql.indexOf('\\') >= 0) {
            throw new SecurityException("The characters $ and \\ are not allowed");
        }

        if (LOCKING_CLAUSE.matcher(code).find()) {
            throw new SecurityException("Row locking clauses are not allowed");
        }

        java.util.regex.Matcher fn = DENIED_FUNCTION.matcher(code);
        if (fn.find()) {
            throw new SecurityException("Forbidden SQL function: " + fn.group(1));
        }
    }

    /**
     * Empty the single-quoted literals (a doubled quote inside one is an escaped quote) and unquote the double-quoted
     * identifiers, keeping their text: {@code "nextval"('s')} is a call of {@code nextval} and must be seen as one.
     * Both kinds of quote are tracked as the database does: in {@code "a'"} the single quote is just a character, and
     * treating it as the start of a literal would hide the SQL that follows from the checks.
     *
     * @throws SecurityException if a quote is not closed (the database would reject it, and the rest is not parseable)
     */
    static String stripQuoted(String sql) {
        StringBuilder out = new StringBuilder(sql.length());
        char open = 0; // 0 = not inside a quoted part, else the quote character that opened it
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (open == 0) {
                if (c == '\'') {
                    out.append(c); // keep the quotes of a literal, drop what is between them
                    open = c;
                } else if (c == '"') {
                    open = c;      // identifier: keep its text, drop the quotes
                } else {
                    out.append(c);
                }
            } else if (c == open) {
                if (i + 1 < sql.length() && sql.charAt(i + 1) == open) {
                    i++;           // doubled quote: an escaped quote, still inside
                    if (open == '"') {
                        out.append('"');
                    }
                } else {
                    if (open == '\'') {
                        out.append(c);
                    }
                    open = 0;
                }
            } else if (open == '"') {
                out.append(c);
            }
        }
        if (open != 0) {
            throw new SecurityException("Unterminated quoted string or identifier");
        }
        return out.toString();
    }
}
