package com.cloudempiere.ai.provider.langchain4j;

/**
 * The caller's role or user may not use the AI provider (ADR-058, CLD-2541).
 *
 * <p>Thrown or reported by {@link IAIService} entry points before any model call, so no tokens are spent.
 * An HTTP or MCP layer maps it to 403.
 */
public class AIAccessDeniedException extends SecurityException {

    private static final long serialVersionUID = 20261104L;

    public static final String MESSAGE = "You do not have access to the AI assistant";

    public AIAccessDeniedException() {
        super(MESSAGE);
    }
}
