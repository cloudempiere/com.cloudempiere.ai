/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 2026 Cloudempiere                                            *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package com.cloudempiere.ai.guardrails;

import org.compiere.util.CLogger;

import com.cloudempiere.ai.guardrails.dto.GuardResult;
import com.cloudempiere.ai.provider.dto.AIStreamCallback;

/**
 * Applies the {@link OutputGuard} to a streamed answer line by line (ADR-014, CLD-2436).
 *
 * <p>LangChain4j's {@code @OutputGuardrails} on a streaming AI service holds back every token until the whole answer
 * has been validated, so the chat showed nothing until the model had finished, and it then released the original
 * tokens, so masking never reached the screen. This decorator holds back only the current unfinished line: each
 * completed line is checked and shown, masked when needed. A line the guard blocks is never shown and ends the
 * answer with an error; the lines before it have passed the same check. Text pending before a tool step is released
 * first, so the order on screen matches the model's, and the text after the step starts a new paragraph. The
 * hallucination note follows the complete answer.
 *
 * <p>Tokens and tool events arrive on different threads, so the methods are synchronized.
 */
public class GuardedStreamCallback implements AIStreamCallback {

    private static final CLogger log = CLogger.getCLogger(GuardedStreamCallback.class);

    private final AIStreamCallback delegate;
    private final OutputGuard guard;
    /** Text of the current line, not yet checked */
    private final StringBuilder pending = new StringBuilder();
    /** Text shown so far, after masking */
    private final StringBuilder released = new StringBuilder();
    private boolean blocked;
    /** Text was shown before a tool step: the text after it starts a new paragraph */
    private boolean paragraphAfterTool;

    /**
     * @param delegate the callback that shows the answer
     * @param guard the output guard
     */
    public GuardedStreamCallback(AIStreamCallback delegate, OutputGuard guard) {
        this.delegate = delegate;
        this.guard = guard;
    }

    @Override
    public synchronized void onChunk(String chunk) {
        if (blocked || chunk == null || chunk.isEmpty()) {
            return;
        }
        if (paragraphAfterTool) {
            paragraphAfterTool = false;
            released.append("\n\n");
            delegate.onChunk("\n\n");
        }
        pending.append(chunk);
        int newline;
        while (!blocked && (newline = pending.indexOf("\n")) >= 0) {
            String line = pending.substring(0, newline + 1);
            pending.delete(0, newline + 1);
            release(line);
        }
    }

    @Override
    public synchronized void onComplete() {
        flush();
        if (blocked) {
            return; // the answer already ended with the block error
        }
        String disclaimer = guard.disclaimerFor(released.toString());
        if (disclaimer != null) {
            boolean endsWithNewline = released.length() > 0 && released.charAt(released.length() - 1) == '\n';
            delegate.onChunk((endsWithNewline ? "\n" : "\n\n") + disclaimer);
        }
        delegate.onComplete();
    }

    @Override
    public synchronized void onError(Exception error) {
        if (blocked) {
            return; // the block was reported already
        }
        flush();
        if (!blocked) {
            delegate.onError(error);
        }
    }

    @Override
    public synchronized void onToolStart(String toolName, String arguments) {
        flush();
        paragraphAfterTool = released.length() > 0 && released.charAt(released.length() - 1) != '\n';
        delegate.onToolStart(toolName, arguments);
    }

    @Override
    public synchronized void onToolComplete(String toolName, String result) {
        delegate.onToolComplete(toolName, result);
    }

    @Override
    public synchronized void onToolError(String toolName, String error) {
        delegate.onToolError(toolName, error);
    }

    @Override
    public synchronized void onThinking(String thinkingChunk) {
        delegate.onThinking(thinkingChunk);
    }

    @Override
    public synchronized void onThinkingComplete() {
        delegate.onThinkingComplete();
    }

    @Override
    public synchronized void onProgress(String stage, String detail) {
        delegate.onProgress(stage, detail);
    }

    /** Checks and shows the text of the unfinished line, e.g. at the end or before a tool step. */
    private void flush() {
        if (blocked || pending.length() == 0) {
            return;
        }
        String rest = pending.toString();
        pending.setLength(0);
        release(rest);
    }

    private void release(String text) {
        GuardResult result = guard.validateFragment(text);
        if (result.getAction() == GuardResult.Action.BLOCK) {
            blocked = true;
            pending.setLength(0);
            log.warning("[STREAM] Output guard blocked the answer: " + result.getViolations());
            delegate.onError(new Exception("Output blocked by the output guard: " + result.getBlockReason()));
            return;
        }
        String shown = result.getProcessedContent();
        released.append(shown);
        delegate.onChunk(shown);
    }
}
