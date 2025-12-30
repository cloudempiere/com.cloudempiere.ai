package com.cloudempiere.ai.health;

/**
 * Interface for prerequisite checks.
 *
 * <p>Implementations verify specific prerequisites required for AI plugin
 * functionality. Checks are categorized by tier and executed during
 * health verification.
 *
 * <p>Design for future extraction to shared library:
 * <ul>
 *   <li>No AI-specific dependencies in interface</li>
 *   <li>Generic enough for any iDempiere plugin</li>
 *   <li>Tier system applicable to various use cases</li>
 * </ul>
 *
 * @see ADR-050: Plugin Health and Prerequisite Verification
 */
public interface IPrerequisiteCheck {

	/**
	 * @return Unique identifier for this check
	 */
	String getCheckId();

	/**
	 * @return Human-readable name for display
	 */
	String getCheckName();

	/**
	 * @return The tier this check belongs to
	 */
	PrerequisiteTier getTier();

	/**
	 * Execute the prerequisite check.
	 *
	 * <p>Implementation notes:
	 * <ul>
	 *   <li>Should be fast (timeout after reasonable period)</li>
	 *   <li>Should not throw exceptions - return failed result instead</li>
	 *   <li>Should provide clear resolution steps on failure</li>
	 *   <li>Should check Adempiere.isStarted() if database access needed</li>
	 * </ul>
	 *
	 * @return CheckResult with status and details
	 */
	CheckResult execute();

	/**
	 * @return true if this check requires database access
	 */
	default boolean requiresDatabase() {
		return true;
	}

	/**
	 * @return Description of what this check verifies
	 */
	default String getDescription() {
		return getCheckName();
	}
}
