package com.cloudempiere.ai.health;

/**
 * Defines prerequisite tiers for AI plugin health checks.
 *
 * <p>Each tier represents a different level of criticality:
 * <ul>
 *   <li>CRITICAL - Plugin cannot function at all without these</li>
 *   <li>REQUIRED - Core features unavailable but plugin can load</li>
 *   <li>OPTIONAL - Enhanced features unavailable, core works</li>
 *   <li>EXTERNAL - Third-party services, graceful degradation expected</li>
 * </ul>
 *
 * @see ADR-050: Plugin Health and Prerequisite Verification
 */
public enum PrerequisiteTier {

	/**
	 * Critical prerequisites - plugin cannot function without these.
	 * Examples: Core database tables (AIG_Provider, AIG_Model)
	 */
	CRITICAL(1, "Critical", true),

	/**
	 * Required prerequisites - core features unavailable but plugin loads.
	 * Examples: pgvector extension, AIG_Embedding table
	 */
	REQUIRED(2, "Required", true),

	/**
	 * Optional prerequisites - enhanced features unavailable.
	 * Examples: Knowledge base tables, audit tables
	 */
	OPTIONAL(3, "Optional", false),

	/**
	 * External prerequisites - third-party services.
	 * Examples: Anthropic API, AWS Bedrock, Ollama
	 */
	EXTERNAL(4, "External", false);

	private final int priority;
	private final String displayName;
	private final boolean blocksStartup;

	PrerequisiteTier(int priority, String displayName, boolean blocksStartup) {
		this.priority = priority;
		this.displayName = displayName;
		this.blocksStartup = blocksStartup;
	}

	/**
	 * @return Priority level (1 = highest priority)
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * @return Human-readable display name
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @return true if failure at this tier should block startup in STRICT mode
	 */
	public boolean blocksStartup() {
		return blocksStartup;
	}
}
