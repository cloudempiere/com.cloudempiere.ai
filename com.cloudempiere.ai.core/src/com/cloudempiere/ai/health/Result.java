package com.cloudempiere.ai.health;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Result wrapper for defensive operations.
 *
 * <p>Encapsulates operation outcomes with three states:
 * <ul>
 *   <li>SUCCESS - Operation completed successfully with value</li>
 *   <li>NOT_AVAILABLE - Service/feature not available (graceful)</li>
 *   <li>ERROR - Operation failed with error details</li>
 * </ul>
 *
 * <p>This pattern ensures UI code never receives raw exceptions,
 * enabling graceful degradation and user-friendly error messages.
 *
 * @param <T> The type of the success value
 * @see ADR-051: ZK UI Defensive Programming
 */
public final class Result<T> {

	public enum State {
		SUCCESS,
		NOT_AVAILABLE,
		ERROR
	}

	private final State state;
	private final T value;
	private final String message;
	private final String errorReference;
	private final Throwable cause;

	private Result(State state, T value, String message, String errorReference, Throwable cause) {
		this.state = Objects.requireNonNull(state, "state");
		this.value = value;
		this.message = message;
		this.errorReference = errorReference;
		this.cause = cause;
	}

	// Static factory methods

	/**
	 * Create a successful result with value.
	 */
	public static <T> Result<T> success(T value) {
		return new Result<>(State.SUCCESS, value, null, null, null);
	}

	/**
	 * Create a not-available result (service unavailable).
	 */
	public static <T> Result<T> notAvailable(String message) {
		return new Result<>(State.NOT_AVAILABLE, null, message, null, null);
	}

	/**
	 * Create an error result with user message.
	 */
	public static <T> Result<T> error(String message) {
		return new Result<>(State.ERROR, null, message, null, null);
	}

	/**
	 * Create an error result with user message and reference ID.
	 */
	public static <T> Result<T> error(String message, String errorReference) {
		return new Result<>(State.ERROR, null, message, errorReference, null);
	}

	/**
	 * Create an error result with full details.
	 */
	public static <T> Result<T> error(String message, String errorReference, Throwable cause) {
		return new Result<>(State.ERROR, null, message, errorReference, cause);
	}

	// State checks

	public boolean isSuccess() {
		return state == State.SUCCESS;
	}

	public boolean isNotAvailable() {
		return state == State.NOT_AVAILABLE;
	}

	public boolean isError() {
		return state == State.ERROR;
	}

	public boolean hasValue() {
		return value != null;
	}

	// Getters

	public State getState() {
		return state;
	}

	/**
	 * Get the value if successful.
	 * @return Optional containing value if success, empty otherwise
	 */
	public Optional<T> getValue() {
		return Optional.ofNullable(value);
	}

	/**
	 * Get value or throw if not successful.
	 * @throws IllegalStateException if not in SUCCESS state
	 */
	public T getValueOrThrow() {
		if (!isSuccess()) {
			throw new IllegalStateException("Result is not success: " + state + " - " + message);
		}
		return value;
	}

	/**
	 * Get value or default if not successful.
	 */
	public T getValueOrDefault(T defaultValue) {
		return isSuccess() && value != null ? value : defaultValue;
	}

	/**
	 * Get user-facing message (for NOT_AVAILABLE or ERROR states).
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Get error reference ID for support/logging (ERROR state only).
	 */
	public Optional<String> getErrorReference() {
		return Optional.ofNullable(errorReference);
	}

	/**
	 * Get underlying exception if any (for logging, not user display).
	 */
	public Optional<Throwable> getCause() {
		return Optional.ofNullable(cause);
	}

	// Functional operations

	/**
	 * Transform value if successful.
	 */
	public <U> Result<U> map(Function<T, U> mapper) {
		if (isSuccess() && value != null) {
			return Result.success(mapper.apply(value));
		}
		@SuppressWarnings("unchecked")
		Result<U> result = (Result<U>) this;
		return result;
	}

	/**
	 * Transform to another Result if successful.
	 */
	public <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
		if (isSuccess() && value != null) {
			return mapper.apply(value);
		}
		@SuppressWarnings("unchecked")
		Result<U> result = (Result<U>) this;
		return result;
	}

	/**
	 * Execute action if successful.
	 */
	public Result<T> onSuccess(Consumer<T> action) {
		if (isSuccess() && value != null) {
			action.accept(value);
		}
		return this;
	}

	/**
	 * Execute action if error.
	 */
	public Result<T> onError(Consumer<String> action) {
		if (isError() && message != null) {
			action.accept(message);
		}
		return this;
	}

	/**
	 * Execute action if not available.
	 */
	public Result<T> onNotAvailable(Consumer<String> action) {
		if (isNotAvailable() && message != null) {
			action.accept(message);
		}
		return this;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Result{state=").append(state);
		if (value != null) {
			sb.append(", value=").append(value);
		}
		if (message != null) {
			sb.append(", message='").append(message).append("'");
		}
		if (errorReference != null) {
			sb.append(", ref=").append(errorReference);
		}
		return sb.append("}").toString();
	}
}
