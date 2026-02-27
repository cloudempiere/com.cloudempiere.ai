package com.cloudempiere.ai.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MIssue;
import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.json.JSONObject;

/**
 * AI Error Handler - Converts technical errors to user-friendly messages
 * and logs detailed information to AD_Issue for debugging.
 *
 * <p>This handler ensures that:
 * <ul>
 *   <li>Users never see raw technical errors or stack traces</li>
 *   <li>Error messages are displayed in the user's language (via AD_Message)</li>
 *   <li>Technical details are preserved in AD_Issue for support</li>
 *   <li>A reference code links user-facing message to technical details</li>
 * </ul>
 *
 * <h3>Error Categories and AD_Message Keys:</h3>
 * <ul>
 *   <li>AIG_Error_RateLimit - Rate limiting / too many requests</li>
 *   <li>AIG_Error_Timeout - Request timeout</li>
 *   <li>AIG_Error_ContentFilter - Content blocked by safety filters</li>
 *   <li>AIG_Error_Configuration - Auth/config errors</li>
 *   <li>AIG_Error_ContextLength - Conversation too long</li>
 *   <li>AIG_Error_ServiceUnavailable - Provider service down</li>
 *   <li>AIG_Error_Generic - Unknown/unexpected errors</li>
 * </ul>
 *
 * @author Cloudempiere
 * @version 1.0
 */
public class AIErrorHandler {

    private static final CLogger log = CLogger.getCLogger(AIErrorHandler.class);

    /** Error category constants */
    public static final String CATEGORY_RATE_LIMIT = "RATE_LIMIT";
    public static final String CATEGORY_TIMEOUT = "TIMEOUT";
    public static final String CATEGORY_CONTENT_FILTER = "CONTENT_FILTER";
    public static final String CATEGORY_CONFIGURATION = "CONFIGURATION";
    public static final String CATEGORY_CONTEXT_LENGTH = "CONTEXT_LENGTH";
    public static final String CATEGORY_SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
    public static final String CATEGORY_GENERIC = "GENERIC";

    /** AD_Message keys for each category */
    private static final String MSG_RATE_LIMIT = "AIG_Error_RateLimit";
    private static final String MSG_TIMEOUT = "AIG_Error_Timeout";
    private static final String MSG_CONTENT_FILTER = "AIG_Error_ContentFilter";
    private static final String MSG_CONFIGURATION = "AIG_Error_Configuration";
    private static final String MSG_CONTEXT_LENGTH = "AIG_Error_ContextLength";
    private static final String MSG_SERVICE_UNAVAILABLE = "AIG_Error_ServiceUnavailable";
    private static final String MSG_GENERIC = "AIG_Error_Generic";

    /** Fallback messages (used if AD_Message not found) - includes user-friendly text + actionable hint */
    private static final String FALLBACK_RATE_LIMIT =
        "I'm receiving too many requests right now. " +
        "\n\n**What you can do:** Wait about 30 seconds and try again.";

    private static final String FALLBACK_TIMEOUT =
        "That request took too long to process. " +
        "\n\n**What you can do:** Try asking a simpler or more specific question.";

    private static final String FALLBACK_CONTENT_FILTER =
        "I can't respond to that request due to content guidelines. " +
        "\n\n**What you can do:** Please rephrase your question differently.";

    private static final String FALLBACK_CONFIGURATION =
        "I'm having trouble connecting to the AI service. " +
        "\n\n**What you can do:** Please contact your system administrator to check the AI provider configuration.";

    private static final String FALLBACK_CONTEXT_LENGTH =
        "Our conversation has become too long for me to process. " +
        "\n\n**What you can do:** Start a new conversation using the 'New Chat' button.";

    private static final String FALLBACK_SERVICE_UNAVAILABLE =
        "The AI service is temporarily unavailable. " +
        "\n\n**What you can do:** Please wait a few minutes and try again. If the problem persists, contact support.";

    private static final String FALLBACK_GENERIC =
        "Something unexpected went wrong while processing your request. " +
        "\n\n**What you can do:** " +
        "\n• Try again - sometimes a retry helps" +
        "\n• Rephrase your question" +
        "\n• If the problem persists, contact support with the error reference shown";

    /**
     * Result object containing user-friendly message and error reference.
     */
    public static class AIErrorResult {
        private final String category;
        private final String userMessage;
        private final String errorReference;
        private final String debugTooltip;
        private final int adIssueId;

        public AIErrorResult(String category, String userMessage, String errorReference,
                           String debugTooltip, int adIssueId) {
            this.category = category;
            this.userMessage = userMessage;
            this.errorReference = errorReference;
            this.debugTooltip = debugTooltip;
            this.adIssueId = adIssueId;
        }

        public String getCategory() { return category; }
        public String getUserMessage() { return userMessage; }
        public String getErrorReference() { return errorReference; }
        public String getDebugTooltip() { return debugTooltip; }
        public int getAD_Issue_ID() { return adIssueId; }

        /**
         * Get the formatted message for display in chat.
         * Includes the user message and a small debug indicator.
         */
        public String getFormattedChatMessage() {
            // Format: User message + small debug emoji with reference
            // The emoji can have a tooltip in the UI showing the error reference
            return userMessage + " \u26A0\uFE0F"; // warning sign (U+26A0 + variation selector)
        }

        /**
         * Get HTML formatted message with tooltip for ZK UI.
         */
        public String getHtmlMessageWithTooltip() {
            return userMessage +
                " <span style=\"cursor:help; opacity:0.6;\" title=\"" + debugTooltip + "\">\u26A0\uFE0F</span>";
        }
    }

    /**
     * Handle an error and return user-friendly result.
     * Creates AD_Issue record and generates reference code.
     *
     * @param ctx iDempiere context
     * @param error the throwable error
     * @param providerName AI provider name (e.g., "AWS Bedrock", "Anthropic")
     * @param userMessage the original user message that caused the error (can be null)
     * @param chatId the CM_Chat_ID (can be 0)
     * @return AIErrorResult with user-friendly message and debug info
     */
    public static AIErrorResult handleError(Properties ctx, Throwable error,
            String providerName, String userMessage, int chatId) {

        // 1. Categorize the error
        String category = categorizeError(error);

        // 2. Generate error reference code
        String errorReference = generateErrorReference(chatId);

        // 3. Create AD_Issue record with full technical details
        int adIssueId = createIssueRecord(ctx, error, category, providerName,
                                          userMessage, chatId, errorReference);

        // 4. Get user-friendly message in user's language
        String friendlyMessage = getUserFriendlyMessage(ctx, category);

        // 5. Build debug tooltip
        String debugTooltip = buildDebugTooltip(errorReference, category, providerName);

        log.warning("AI Error handled: category=" + category + ", ref=" + errorReference +
                   ", issueId=" + adIssueId);

        return new AIErrorResult(category, friendlyMessage, errorReference, debugTooltip, adIssueId);
    }

    /**
     * Categorize the error based on message content and exception type.
     */
    public static String categorizeError(Throwable error) {
        if (error == null) {
            return CATEGORY_GENERIC;
        }

        String message = getFullErrorMessage(error).toLowerCase();
        String className = error.getClass().getName().toLowerCase();

        // Rate limiting
        if (message.contains("rate_limit") || message.contains("rate limit") ||
            message.contains("too many requests") || message.contains("429") ||
            message.contains("throttl")) {
            return CATEGORY_RATE_LIMIT;
        }

        // Timeout
        if (message.contains("timeout") || message.contains("timed out") ||
            message.contains("deadline exceeded") ||
            className.contains("timeout") || className.contains("sockettimeout")) {
            return CATEGORY_TIMEOUT;
        }

        // Content filtering
        if (message.contains("content filter") || message.contains("blocked") ||
            message.contains("safety") || message.contains("policy violation") ||
            message.contains("harmful") || message.contains("inappropriate")) {
            return CATEGORY_CONTENT_FILTER;
        }

        // Authentication/Configuration
        if (message.contains("authentication") || message.contains("api_key") ||
            message.contains("unauthorized") || message.contains("403") ||
            message.contains("401") || message.contains("invalid_api_key") ||
            message.contains("credential") || message.contains("permission denied")) {
            return CATEGORY_CONFIGURATION;
        }

        // Context length
        if (message.contains("context_length") || message.contains("context length") ||
            message.contains("too long") || message.contains("max_tokens") ||
            message.contains("token limit") || message.contains("maximum context")) {
            return CATEGORY_CONTEXT_LENGTH;
        }

        // Service unavailable
        if (message.contains("service unavailable") || message.contains("503") ||
            message.contains("500") || message.contains("internal_error") ||
            message.contains("server_error") || message.contains("overloaded") ||
            message.contains("capacity")) {
            return CATEGORY_SERVICE_UNAVAILABLE;
        }

        return CATEGORY_GENERIC;
    }

    /**
     * Get the full error message including cause chain.
     */
    private static String getFullErrorMessage(Throwable error) {
        StringBuilder sb = new StringBuilder();
        Throwable current = error;
        while (current != null) {
            if (current.getMessage() != null) {
                sb.append(current.getMessage()).append(" ");
            }
            sb.append(current.getClass().getName()).append(" ");
            current = current.getCause();
        }
        return sb.toString();
    }

    /**
     * Generate a unique error reference code.
     * Format: AIG-{timestamp}-{random4}
     */
    private static String generateErrorReference(int chatId) {
        long timestamp = System.currentTimeMillis() / 1000; // Unix timestamp
        int random = (int) (Math.random() * 10000);
        return String.format("AIG-%d-%04d", timestamp, random);
    }

    /**
     * Get user-friendly message from AD_Message or fallback.
     */
    private static String getUserFriendlyMessage(Properties ctx, String category) {
        String msgKey;
        String fallback;

        switch (category) {
            case CATEGORY_RATE_LIMIT:
                msgKey = MSG_RATE_LIMIT;
                fallback = FALLBACK_RATE_LIMIT;
                break;
            case CATEGORY_TIMEOUT:
                msgKey = MSG_TIMEOUT;
                fallback = FALLBACK_TIMEOUT;
                break;
            case CATEGORY_CONTENT_FILTER:
                msgKey = MSG_CONTENT_FILTER;
                fallback = FALLBACK_CONTENT_FILTER;
                break;
            case CATEGORY_CONFIGURATION:
                msgKey = MSG_CONFIGURATION;
                fallback = FALLBACK_CONFIGURATION;
                break;
            case CATEGORY_CONTEXT_LENGTH:
                msgKey = MSG_CONTEXT_LENGTH;
                fallback = FALLBACK_CONTEXT_LENGTH;
                break;
            case CATEGORY_SERVICE_UNAVAILABLE:
                msgKey = MSG_SERVICE_UNAVAILABLE;
                fallback = FALLBACK_SERVICE_UNAVAILABLE;
                break;
            default:
                msgKey = MSG_GENERIC;
                fallback = FALLBACK_GENERIC;
        }

        // Try to get translated message from AD_Message
        String translated = Msg.getMsg(ctx, msgKey);

        // If not found (returns key), use fallback
        if (translated == null || translated.equals(msgKey)) {
            return fallback;
        }

        return translated;
    }

    /**
     * Build debug tooltip text.
     */
    private static String buildDebugTooltip(String errorReference, String category, String providerName) {
        return String.format("Ref: %s | Category: %s | Provider: %s",
                           errorReference, category, providerName != null ? providerName : "Unknown");
    }

    /**
     * Create AD_Issue record with full technical details.
     *
     * @return AD_Issue_ID or 0 if creation failed
     */
    private static int createIssueRecord(Properties ctx, Throwable error, String category,
            String providerName, String userMessage, int chatId, String errorReference) {

        try {
            MIssue issue = new MIssue(ctx, 0, null);

            // Set summary with error reference for easy lookup
            String summary = String.format("[%s] AI Error: %s - %s",
                errorReference, category,
                error.getMessage() != null ? truncate(error.getMessage(), 200) : error.getClass().getSimpleName());
            issue.setIssueSummary(summary);

            // Set source information
            issue.setSourceClassName("com.cloudempiere.ai");
            issue.setSourceMethodName(category);
            issue.setLoggerName("AIErrorHandler");
            issue.setName(errorReference);

            // Set stack trace
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            error.printStackTrace(pw);
            String stackTrace = sw.toString();
            issue.setStackTrace(truncate(stackTrace, 2000));

            // Set error trace (first few relevant lines)
            issue.setErrorTrace(truncate(getRelevantStackTrace(error), 2000));

            // Build comments with context
            StringBuilder comments = new StringBuilder();
            comments.append("=== AI Error Report ===\n");
            comments.append("Error Reference: ").append(errorReference).append("\n");
            comments.append("Category: ").append(category).append("\n");
            comments.append("Provider: ").append(providerName != null ? providerName : "Unknown").append("\n");
            comments.append("Chat ID: ").append(chatId > 0 ? chatId : "N/A").append("\n");
            comments.append("Timestamp: ").append(new Timestamp(System.currentTimeMillis())).append("\n");
            comments.append("\n--- User Message ---\n");
            comments.append(userMessage != null ? truncate(userMessage, 500) : "(none)").append("\n");
            comments.append("\n--- Error Details ---\n");
            comments.append("Exception: ").append(error.getClass().getName()).append("\n");
            comments.append("Message: ").append(error.getMessage()).append("\n");

            // Add cause chain
            Throwable cause = error.getCause();
            int causeDepth = 0;
            while (cause != null && causeDepth < 5) {
                comments.append("Caused by: ").append(cause.getClass().getName())
                       .append(": ").append(cause.getMessage()).append("\n");
                cause = cause.getCause();
                causeDepth++;
            }

            issue.setComments(truncate(comments.toString(), 2000));

            // Set user context if available
            int userId = Env.getAD_User_ID(ctx);
            if (userId > 0) {
                MUser user = MUser.get(ctx, userId);
                if (user != null) {
                    issue.setUserName(user.getName());
                }
            }

            // Save the issue
            if (issue.save()) {
                log.info("Created AD_Issue record: " + issue.getAD_Issue_ID() + " for " + errorReference);
                return issue.getAD_Issue_ID();
            } else {
                log.warning("Failed to save AD_Issue for " + errorReference);
            }

        } catch (Exception e) {
            // Don't let issue creation failure affect user experience
            log.log(Level.SEVERE, "Failed to create AD_Issue record", e);
        }

        return 0;
    }

    /**
     * Get relevant stack trace lines (cloudempiere/adempiere related).
     */
    private static String getRelevantStackTrace(Throwable error) {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] elements = error.getStackTrace();
        int count = 0;

        for (StackTraceElement element : elements) {
            String line = element.toString();
            if (line.contains("cloudempiere") || line.contains("adempiere") ||
                line.contains("idempiere") || line.contains("langchain")) {
                sb.append(line).append("\n");
                count++;
                if (count >= 10) break;
            }
        }

        if (sb.length() == 0) {
            // No relevant lines found, take first few
            for (int i = 0; i < Math.min(5, elements.length); i++) {
                sb.append(elements[i].toString()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Truncate string to max length.
     */
    private static String truncate(String str, int maxLength) {
        if (str == null) return null;
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Parse error from JSON response if present.
     * Handles various API error formats (Anthropic, OpenAI, Bedrock).
     */
    public static String parseJsonError(String message) {
        if (message == null || !message.contains("{")) {
            return message;
        }

        try {
            JSONObject json = new JSONObject(message);

            // Anthropic format: {"type":"error","error":{"message":"..."}}
            if (json.has("error")) {
                Object errorObj = json.get("error");
                if (errorObj instanceof JSONObject) {
                    JSONObject errorJson = (JSONObject) errorObj;
                    return errorJson.optString("message", message);
                } else if (errorObj instanceof String) {
                    return (String) errorObj;
                }
            }

            // OpenAI format: {"error":{"message":"...","type":"..."}}
            // Already handled above

            // Generic: {"message":"..."}
            if (json.has("message")) {
                return json.getString("message");
            }

        } catch (Exception e) {
            // Not valid JSON, return original
        }

        return message;
    }
}
