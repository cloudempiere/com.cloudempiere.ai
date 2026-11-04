package com.cloudempiere.ai.provider.langchain4j.mcp;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code ${env:NAME}} references in MCP server configuration (CLD-2543), so tokens live in the server's
 * environment (ECS task definition, secrets store) and never as plain text in AD_SysConfig.
 *
 * <p>A reference to a variable that is not set fails the whole server entry: connecting with an empty token
 * would only fail later with a less clear error. Error messages name the variable, never a value.
 */
public final class McpSecrets {

    private static final Pattern ENV_REF = Pattern.compile("\\$\\{env:([A-Za-z_][A-Za-z0-9_]*)}");

    private final Function<String, String> env;

    /** @param env variable lookup, {@code System::getenv} in production */
    public McpSecrets(Function<String, String> env) {
        this.env = env;
    }

    /**
     * Replace every {@code ${env:NAME}} in {@code value}.
     *
     * @return the resolved value; null stays null
     * @throws IllegalArgumentException when a referenced variable is not set
     */
    public String resolve(String value) {
        if (value == null || value.indexOf("${env:") < 0) {
            return value;
        }
        Matcher matcher = ENV_REF.matcher(value);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            String resolved = env.apply(name);
            if (resolved == null) {
                throw new IllegalArgumentException("environment variable " + name + " is not set");
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(resolved));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
