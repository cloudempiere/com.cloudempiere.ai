<%@ page contentType="text/css;charset=UTF-8" %>
<%@ taglib uri="http://www.zkoss.org/dsp/web/core" prefix="c" %>
/**
 * CloudEmpiere AI Plugin - Custom Theme Fragment
 *
 * This file uses iDempiere's theme extension point (NF8.2).
 * The main theme.css.dsp automatically includes fragment/custom.css.dsp
 * if it exists in any fragment bundle attached to org.adempiere.ui.zk.
 *
 * See: https://wiki.idempiere.org/en/NF8.2_Lightweight_theme_customization
 *
 * Usage in ZK components:
 * - Java: this.setSclass("ai-chat-widget");
 * - ZUL:  <div sclass="ai-chat-panel">...</div>
 */

/* Main chat panel container */
.ai-chat-panel {
    display: flex;
    flex-direction: column;
    height: 100%;
    background-color: #ffffff;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* Chat header */
.ai-chat-header {
    padding: 16px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #ffffff;
    border-top-left-radius: 8px;
    border-top-right-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.ai-chat-header-title {
    font-size: 18px;
    font-weight: 600;
}

/* Messages container */
.ai-chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
    background-color: #f9fafb;
}

/* Individual message bubble */
.ai-message {
    margin-bottom: 12px;
    display: flex;
    flex-direction: column;
}

.ai-message-user {
    align-items: flex-end;
}

.ai-message-assistant {
    align-items: flex-start;
}

.ai-message-bubble {
    max-width: 70%;
    padding: 12px 16px;
    border-radius: 18px;
    word-wrap: break-word;
}

.ai-message-user .ai-message-bubble {
    background-color: #667eea;
    color: #ffffff;
}

.ai-message-assistant .ai-message-bubble {
    background-color: #ffffff;
    color: #1f2937;
    border: 1px solid #e5e7eb;
}

/* Input area */
.ai-chat-input-container {
    padding: 16px;
    background-color: #ffffff;
    border-top: 1px solid #e0e0e0;
    border-bottom-left-radius: 8px;
    border-bottom-right-radius: 8px;
}

.ai-chat-input {
    width: 100%;
    min-height: 44px;
    padding: 12px;
    border: 1px solid #d1d5db;
    border-radius: 8px;
    resize: vertical;
    font-family: inherit;
    font-size: 14px;
}

.ai-chat-input:focus {
    outline: none;
    border-color: #667eea;
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

/* Provider indicators */
.ai-provider-badge {
    display: inline-flex;
    align-items: center;
    padding: 4px 8px;
    background-color: #f3f4f6;
    border-radius: 12px;
    font-size: 12px;
    font-weight: 500;
    color: #6b7280;
}

.ai-provider-badge img {
    width: 16px;
    height: 16px;
    margin-right: 4px;
}

/* Streaming indicator */
.ai-streaming-indicator {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 8px 12px;
    background-color: #eff6ff;
    border-radius: 12px;
    color: #1e40af;
    font-size: 13px;
}

.ai-streaming-dot {
    width: 6px;
    height: 6px;
    background-color: #3b82f6;
    border-radius: 50%;
    animation: pulse 1.5s ease-in-out infinite;
}

.ai-streaming-dot:nth-child(2) {
    animation-delay: 0.2s;
}

.ai-streaming-dot:nth-child(3) {
    animation-delay: 0.4s;
}

@keyframes pulse {
    0%, 100% {
        opacity: 0.3;
        transform: scale(0.8);
    }
    50% {
        opacity: 1;
        transform: scale(1.2);
    }
}

/* Error states */
.ai-error-message {
    color: #d32f2f;
    padding: 8px;
    background: #ffebee;
    border-radius: 4px;
    border-left: 3px solid #d32f2f;
}

.ai-warning-message {
    color: #ed6c02;
    padding: 8px;
    background: #fff4e5;
    border-radius: 4px;
    border-left: 3px solid #ed6c02;
}

.ai-warning-inline {
    color: #ed6c02;
    padding: 6px 10px;
    margin-bottom: 8px;
    background: #fff4e5;
    border-radius: 4px;
    font-size: 12px;
}

/* Loading states */
.ai-loading {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 24px;
}

.ai-loading-spinner {
    border: 3px solid #f3f4f6;
    border-top: 3px solid #667eea;
    border-radius: 50%;
    width: 40px;
    height: 40px;
    animation: spin 1s linear infinite;
}

@keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
}

/* AI Chat Widget container - replaces inline styles */
.ai-chat-widget {
    display: flex;
    flex-direction: column;
    padding: 12px;
    background: #FDFDFD;
    border-radius: 12px;
    height: 100%;
}

/* Thread control bar */
.ai-thread-control-bar {
    display: flex !important;
    flex-direction: row !important;
    justify-content: space-between !important;
    width: 100%;
    gap: 8px;
    align-items: center;
    margin-bottom: 8px;
    flex-shrink: 0;
}

/* Thread selector */
.ai-thread-selector {
    flex: 1 !important;
    border: 1px solid #E0E0E0;
    border-radius: 6px;
    font-size: 12px;
    background: #FFFFFF;
}

/* New thread button */
.ai-newthread-btn {
    flex-shrink: 0 !important;
    background: #FFFFFF !important;
    border: 1px solid #E0E0E0 !important;
    border-radius: 6px !important;
    padding: 6px 12px !important;
    font-size: 13px !important;
    font-weight: 500 !important;
    color: #181D27 !important;
    cursor: pointer !important;
    display: inline-flex !important;
    align-items: center !important;
    gap: 6px !important;
    transition: all 0.2s ease !important;
}

.ai-newthread-btn:hover {
    background: #F5F5F5 !important;
    border-color: #CCCCCC !important;
}

.ai-newthread-btn:active {
    background: #EEEEEE !important;
    transform: scale(0.98);
}

/* Messages container */
.ai-messages {
    overflow-y: auto !important;
    overflow-x: hidden !important;
    margin-bottom: 12px !important;
    padding: 6px !important;
    gap: 8px !important;
    flex: 1 1 auto !important;
    min-height: 0 !important;
}

/* Input area container */
.ai-input-area {
    width: 100%;
    gap: 8px;
    align-items: center;
    flex-shrink: 0;
}

/* Input textbox */
.ai-input-box {
    border: 1px solid rgba(122, 128, 140, 0.32) !important;
    border-radius: 32px !important;
    padding: 12px 18px !important;
    font-size: 12px !important;
    line-height: 18px !important;
    color: #717680 !important;
    background: #FFFFFF !important;
}

/* Button container */
.ai-button-container {
    position: relative;
    width: 42px;
    height: 42px;
}

/* Send button */
.ai-send-btn {
    position: absolute !important;
    top: 0 !important;
    left: 0 !important;
    width: 42px !important;
    height: 42px !important;
    background: #181D27 !important;
    border-radius: 100px !important;
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
    border: none !important;
    cursor: pointer !important;
    transition: all 0.2s ease !important;
}

.ai-send-btn:hover {
    background: #2A2F3A !important;
    transform: scale(1.05);
}

.ai-send-btn:active {
    transform: scale(0.95);
}

/* Stop button */
.ai-stop-btn {
    position: absolute !important;
    top: 0 !important;
    left: 0 !important;
    width: 42px !important;
    height: 42px !important;
    background: #D32F2F !important;
    border-radius: 100px !important;
    /* display controlled by ai-hidden/ai-visible classes */
    align-items: center !important;
    justify-content: center !important;
    border: none !important;
    cursor: pointer !important;
    transition: all 0.2s ease !important;
    animation: pulse-stop 2s ease-in-out infinite;
}

.ai-stop-btn:hover {
    background: #E53935 !important;
    transform: scale(1.05);
    animation: none;
}

.ai-stop-btn:active {
    transform: scale(0.95);
}

@keyframes pulse-stop {
    0%, 100% {
        box-shadow: 0 0 0 0 rgba(211, 47, 47, 0.7);
    }
    50% {
        box-shadow: 0 0 0 8px rgba(211, 47, 47, 0);
    }
}

/* Clear button */
.ai-clear-btn {
    /* Inherits from standard button styles */
}

/* Context indicator */
.ai-context-indicator {
    padding: 6px 12px;
    background: #E8F5E9;
    border-radius: 4px;
    margin-bottom: 8px;
    font-size: 11px;
    color: #2E7D32;
    display: none;
}

/* Access indicator */
.ai-access-indicator {
    padding: 6px 12px;
    background: #FFF3E0;
    border-radius: 4px;
    margin-bottom: 8px;
    font-size: 11px;
    color: #E65100;
}

/* Loading indicator */
.ai-loading-container {
    display: none;
    padding: 12px 18px;
    text-align: left;
}

.ai-loading-content {
    display: flex;
    align-items: center;
    gap: 8px;
}

.ai-loading-avatar {
    width: 18px;
    height: 18px;
    border-radius: 27px;
    background: #E9EAEB;
}

.ai-loading-text {
    font-family: Helvetica Neue;
    font-weight: 400;
    font-size: 12px;
    line-height: 18px;
    color: #717680;
}

/* User message */
.user-message {
    margin-bottom: 12px;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
}

/* Visibility control classes */
.ai-hidden {
    display: none !important;
}

.ai-visible {
    display: flex !important;
}

/* Message content styling */
.ai-message-content {
    display: flex !important;
    flex-direction: column !important;
    gap: 12px !important;
    max-width: 100% !important;
    padding: 12px 18px !important;
    background: transparent !important;
    border-radius: 0 !important;
    width: 100% !important;
}

.user-message-content {
    display: flex !important;
    flex-direction: column !important;
    gap: 12px !important;
    width: 100% !important;
    max-width: 100% !important;
    padding: 12px 15px !important;
    background: #F5F5F5 !important;
    border-radius: 15px !important;
    align-self: flex-start !important;
}

/* Streaming message container */
.ai-streaming-message {
    display: flex !important;
    flex-direction: column !important;
    gap: 12px !important;
    max-width: 100% !important;
    width: 100% !important;
    padding: 12px 18px !important;
    background: transparent !important;
}

/* Message header */
.ai-message-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
}

.ai-message-header-logo {
    width: 18px;
    height: 18px;
}

.ai-message-header-name {
    font-family: Helvetica Neue;
    font-weight: 500;
    font-size: 12px;
    color: #181D27;
}

/* Unavailable message container */
.ai-unavailable-container {
    display: flex !important;
    flex-direction: column !important;
    align-items: center !important;
    justify-content: center !important;
    height: 100% !important;
    padding: 24px !important;
    text-align: center !important;
}

.ai-unavailable-icon {
    font-size: 48px;
    margin-bottom: 16px;
    opacity: 0.5;
}

.ai-unavailable-title {
    font-size: 16px;
    font-weight: 600;
    color: #424242;
    margin-bottom: 8px;
}

.ai-unavailable-message {
    font-size: 13px;
    color: #757575;
    max-width: 300px;
    line-height: 1.5;
}

/* Retry button */
.ai-retry-btn {
    margin-top: 16px !important;
    padding: 8px 16px !important;
    background: #1976d2 !important;
    color: white !important;
    border: none !important;
    border-radius: 4px !important;
    cursor: pointer !important;
}

/* Thread selector separator */
.ai-thread-separator {
    font-style: italic !important;
    color: #888 !important;
}

/* Message body text */
.ai-message-body {
    font-family: Helvetica Neue;
    font-weight: 400;
    font-size: 12px;
    line-height: 18px;
    color: #181D27;
}

.user-message-body {
    font-family: Helvetica Neue;
    font-weight: 400;
    font-size: 12px;
    line-height: 18px;
    color: #535862;
}

/* Message divider */
.ai-message-divider {
    width: 100%;
    height: 0;
    border: 1px solid rgba(24, 29, 39, 0.12);
    margin: 0;
}

/* Copy button */
.ai-copy-button-container {
    display: flex;
    flex-direction: row;
    align-items: flex-start;
    padding: 0;
    gap: 18px;
    margin-top: 12px;
}

.ai-copy-button {
    display: flex;
    flex-direction: row;
    justify-content: center;
    align-items: center;
    padding: 0;
    gap: 6px;
    cursor: pointer;
}

.ai-copy-icon {
    font-size: 12px;
    color: #717680;
}

.ai-copy-text {
    font-family: Helvetica Neue;
    font-weight: 500;
    font-size: 10.5px;
    line-height: 13.5px;
    color: #717680;
}

.ai-copy-success {
    font-family: Helvetica Neue;
    font-weight: 500;
    font-size: 10.5px;
    line-height: 13.5px;
    color: #4CAF50;
}
