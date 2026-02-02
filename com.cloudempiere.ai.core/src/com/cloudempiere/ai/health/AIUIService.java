package com.cloudempiere.ai.health;

import java.util.function.Supplier;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.Msg;

import com.cloudempiere.ai.error.AIErrorHandler;

/**
 * Defensive wrapper for AI operations in UI context.
 *
 * <p>This service provides a safe execution layer that:
 * <ul>
 *   <li>Never throws exceptions to UI code</li>
 *   <li>Checks plugin health before operations</li>
 *   <li>Converts all errors to user-friendly messages</li>
 *   <li>Provides consistent Result&lt;T&gt; return types</li>
 * </ul>
 *
 * <p>Usage in ZK components:
 * <pre>
 * Result&lt;String&gt; result = AIUIService.safeExecute(
 *     () -&gt; aiService.generateResponse(prompt),
 *     "chat"
 * );
 *
 * result.onSuccess(response -&gt; updateUI(response))
 *       .onNotAvailable(msg -&gt; showUnavailableMessage(msg))
 *       .onError(msg -&gt; showErrorMessage(msg));
 * </pre>
 *
 * @see ADR-051: ZK UI Defensive Programming
 * @see Result
 */
public final class AIUIService {

	private static final CLogger log = CLogger.getCLogger(AIUIService.class);

	/** AD_Message key for service unavailable */
	private static final String MSG_SERVICE_UNAVAILABLE = "AIG_ServiceUnavailable";

	/** AD_Message key for generic error */
	private static final String MSG_GENERIC_ERROR = "AIG_Error_Generic";

	private AIUIService() {
		// Utility class
	}

	/**
	 * Execute an AI operation safely.
	 *
	 * <p>This method:
	 * <ol>
	 *   <li>Checks if AI plugin is healthy</li>
	 *   <li>Executes the operation</li>
	 *   <li>Catches all exceptions and converts to user-friendly messages</li>
	 *   <li>Returns a Result that never throws</li>
	 * </ol>
	 *
	 * @param <T> Return type of the operation
	 * @param operation The AI operation to execute
	 * @param featureName Name of the feature for error context (e.g., "chat", "embedding")
	 * @return Result containing success value or error information
	 */
	public static <T> Result<T> safeExecute(Supplier<T> operation, String featureName) {
		// Check plugin health first
		AIPluginHealthService health = AIPluginHealthService.getInstance();
		if (health == null) {
			return Result.notAvailable(getUnavailableMessage());
		}

		if (!health.isHealthy()) {
			String statusMsg = health.getStatusMessage();
			if (statusMsg == null || statusMsg.isEmpty()) {
				statusMsg = getUnavailableMessage();
			}
			return Result.notAvailable(statusMsg);
		}

		// Execute the operation safely
		try {
			T result = operation.get();
			return Result.success(result);
		} catch (Exception e) {
			return handleException(e, featureName);
		}
	}

	/**
	 * Execute an AI operation safely with custom context.
	 *
	 * @param <T> Return type of the operation
	 * @param operation The AI operation to execute
	 * @param featureName Name of the feature for error context
	 * @param context Additional context for error handling
	 * @return Result containing success value or error information
	 */
	public static <T> Result<T> safeExecute(Supplier<T> operation, String featureName, String context) {
		AIPluginHealthService health = AIPluginHealthService.getInstance();
		if (health == null || !health.isHealthy()) {
			String msg = health != null ? health.getStatusMessage() : getUnavailableMessage();
			return Result.notAvailable(msg);
		}

		try {
			T result = operation.get();
			return Result.success(result);
		} catch (Exception e) {
			return handleException(e, featureName, context);
		}
	}

	/**
	 * Execute a void AI operation safely.
	 *
	 * @param operation The AI operation to execute
	 * @param featureName Name of the feature for error context
	 * @return Result indicating success or error
	 */
	public static Result<Void> safeExecuteVoid(Runnable operation, String featureName) {
		return safeExecute(() -> {
			operation.run();
			return null;
		}, featureName);
	}

	/**
	 * Check if AI features are available.
	 *
	 * @return Result with availability status
	 */
	public static Result<Boolean> checkAvailability() {
		AIPluginHealthService health = AIPluginHealthService.getInstance();
		if (health == null) {
			return Result.notAvailable(getUnavailableMessage());
		}

		if (!health.isHealthy()) {
			return Result.notAvailable(health.getStatusMessage());
		}

		return Result.success(true);
	}

	/**
	 * Get user-friendly status for display.
	 *
	 * @return Status message suitable for UI display
	 */
	public static String getDisplayStatus() {
		AIPluginHealthService health = AIPluginHealthService.getInstance();
		if (health == null) {
			return getUnavailableMessage();
		}
		return health.getStatusMessage();
	}

	/**
	 * Handle exception and convert to Result.
	 */
	private static <T> Result<T> handleException(Exception e, String featureName) {
		return handleException(e, featureName, null);
	}

	/**
	 * Handle exception and convert to Result with context.
	 */
	private static <T> Result<T> handleException(Exception e, String featureName, String context) {
		// Log the full exception
		log.log(Level.WARNING, "AI operation failed: " + featureName +
				(context != null ? " [" + context + "]" : ""), e);

		// Use AIErrorHandler if available
		try {
			// AIErrorHandler.handleError(ctx, error, providerName, userMessage, chatId)
			AIErrorHandler.AIErrorResult errorResult = AIErrorHandler.handleError(
					org.compiere.util.Env.getCtx(),
					e,
					featureName,  // use feature name as provider name
					context,      // use context as user message
					0             // no chat ID in this context
			);
			return Result.error(
					errorResult.getUserMessage(),
					errorResult.getErrorReference(),
					e
			);
		} catch (Exception handlerError) {
			// Fallback if error handler fails
			log.log(Level.SEVERE, "Error handler failed", handlerError);
			return Result.error(getGenericErrorMessage(), null, e);
		}
	}

	/**
	 * Get localized unavailable message.
	 */
	private static String getUnavailableMessage() {
		try {
			String msg = Msg.getMsg(org.compiere.util.Env.getCtx(), MSG_SERVICE_UNAVAILABLE);
			if (msg != null && !msg.equals(MSG_SERVICE_UNAVAILABLE)) {
				return msg;
			}
		} catch (Exception e) {
			// Ignore - use fallback
		}
		return "AI features are currently unavailable. Please try again later.";
	}

	/**
	 * Get localized generic error message.
	 */
	private static String getGenericErrorMessage() {
		try {
			String msg = Msg.getMsg(org.compiere.util.Env.getCtx(), MSG_GENERIC_ERROR);
			if (msg != null && !msg.equals(MSG_GENERIC_ERROR)) {
				return msg;
			}
		} catch (Exception e) {
			// Ignore - use fallback
		}
		return "An unexpected error occurred. Please try again.";
	}
}
