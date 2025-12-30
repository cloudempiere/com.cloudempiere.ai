package com.cloudempiere.ai.health;

import java.util.Objects;

/**
 * Result of a prerequisite check.
 *
 * <p>Immutable value object containing the outcome of a health check,
 * including status, message, and resolution steps if applicable.
 *
 * @see ADR-050: Plugin Health and Prerequisite Verification
 */
public final class CheckResult {

	public enum Status {
		/** Check passed successfully */
		PASSED,
		/** Check failed */
		FAILED,
		/** Check was skipped (e.g., not applicable) */
		SKIPPED,
		/** Check could not be performed (e.g., database not ready) */
		UNAVAILABLE
	}

	private final Status status;
	private final String checkName;
	private final String message;
	private final String resolutionSteps;
	private final PrerequisiteTier tier;
	private final long checkDurationMs;
	private final Throwable cause;

	private CheckResult(Builder builder) {
		this.status = Objects.requireNonNull(builder.status, "status");
		this.checkName = Objects.requireNonNull(builder.checkName, "checkName");
		this.message = builder.message;
		this.resolutionSteps = builder.resolutionSteps;
		this.tier = builder.tier;
		this.checkDurationMs = builder.checkDurationMs;
		this.cause = builder.cause;
	}

	// Static factory methods

	public static CheckResult passed(String checkName, PrerequisiteTier tier) {
		return new Builder(checkName, Status.PASSED)
				.tier(tier)
				.message("Check passed")
				.build();
	}

	public static CheckResult passed(String checkName, PrerequisiteTier tier, String message) {
		return new Builder(checkName, Status.PASSED)
				.tier(tier)
				.message(message)
				.build();
	}

	public static CheckResult failed(String checkName, PrerequisiteTier tier, String message) {
		return new Builder(checkName, Status.FAILED)
				.tier(tier)
				.message(message)
				.build();
	}

	public static CheckResult failed(String checkName, PrerequisiteTier tier, String message, String resolution) {
		return new Builder(checkName, Status.FAILED)
				.tier(tier)
				.message(message)
				.resolutionSteps(resolution)
				.build();
	}

	public static CheckResult failed(String checkName, PrerequisiteTier tier, String message, Throwable cause) {
		return new Builder(checkName, Status.FAILED)
				.tier(tier)
				.message(message)
				.cause(cause)
				.build();
	}

	public static CheckResult unavailable(String checkName, PrerequisiteTier tier, String reason) {
		return new Builder(checkName, Status.UNAVAILABLE)
				.tier(tier)
				.message(reason)
				.build();
	}

	public static CheckResult skipped(String checkName, PrerequisiteTier tier, String reason) {
		return new Builder(checkName, Status.SKIPPED)
				.tier(tier)
				.message(reason)
				.build();
	}

	// Getters

	public Status getStatus() {
		return status;
	}

	public String getCheckName() {
		return checkName;
	}

	public String getMessage() {
		return message;
	}

	public String getResolutionSteps() {
		return resolutionSteps;
	}

	public PrerequisiteTier getTier() {
		return tier;
	}

	public long getCheckDurationMs() {
		return checkDurationMs;
	}

	public Throwable getCause() {
		return cause;
	}

	public boolean isPassed() {
		return status == Status.PASSED;
	}

	public boolean isFailed() {
		return status == Status.FAILED;
	}

	public boolean hasResolution() {
		return resolutionSteps != null && !resolutionSteps.isEmpty();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[").append(tier != null ? tier.getDisplayName() : "?").append("] ");
		sb.append(checkName).append(": ").append(status);
		if (message != null) {
			sb.append(" - ").append(message);
		}
		return sb.toString();
	}

	// Builder

	public static class Builder {
		private final String checkName;
		private final Status status;
		private String message;
		private String resolutionSteps;
		private PrerequisiteTier tier;
		private long checkDurationMs;
		private Throwable cause;

		public Builder(String checkName, Status status) {
			this.checkName = checkName;
			this.status = status;
		}

		public Builder message(String message) {
			this.message = message;
			return this;
		}

		public Builder resolutionSteps(String resolutionSteps) {
			this.resolutionSteps = resolutionSteps;
			return this;
		}

		public Builder tier(PrerequisiteTier tier) {
			this.tier = tier;
			return this;
		}

		public Builder checkDurationMs(long checkDurationMs) {
			this.checkDurationMs = checkDurationMs;
			return this;
		}

		public Builder cause(Throwable cause) {
			this.cause = cause;
			return this;
		}

		public CheckResult build() {
			return new CheckResult(this);
		}
	}
}
