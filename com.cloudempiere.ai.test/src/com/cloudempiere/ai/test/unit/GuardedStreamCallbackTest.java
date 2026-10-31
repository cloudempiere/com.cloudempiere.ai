/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                      *
 * Copyright (C) Cloudempiere, Inc. All Rights Reserved.                     *
 * This program is free software; you can redistribute it and/or modify it   *
 * under the terms version 2 of the GNU General Public License as published  *
 * by the Free Software Foundation. This program is distributed in the hope  *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied*
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.          *
 * See the GNU General Public License for more details.                      *
 * You should have received a copy of the GNU General Public License along   *
 * with this program; if not, write to the Free Software Foundation, Inc.,   *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                    *
 *****************************************************************************/
package com.cloudempiere.ai.test.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.guardrails.GuardedStreamCallback;
import com.cloudempiere.ai.guardrails.OutputGuard;
import com.cloudempiere.ai.guardrails.dto.GuardResult;
import com.cloudempiere.ai.provider.dto.AIStreamCallback;
import com.cloudempiere.ai.test.categories.UnitTest;

/**
 * The output guard on a streamed answer (CLD-2436): LangChain4j's {@code @OutputGuardrails} held back every token
 * until the answer was complete, so the chat showed nothing while the model wrote. The guard now checks each line as
 * soon as it is complete; only the unfinished line is held back.
 */
@UnitTest
@DisplayName("Output guard on a streamed answer")
class GuardedStreamCallbackTest {

    /** Records what reaches the chat, in order */
    private final List<String> events = new ArrayList<>();
    private final StringBuilder shown = new StringBuilder();

    private final AIStreamCallback chat = new AIStreamCallback() {
        @Override
        public void onChunk(String chunk) {
            shown.append(chunk);
            events.add("chunk:" + chunk);
        }

        @Override
        public void onComplete() {
            events.add("complete");
        }

        @Override
        public void onError(Exception error) {
            events.add("error:" + error.getMessage());
        }

        @Override
        public void onToolStart(String toolName, String arguments) {
            events.add("tool:" + toolName);
        }
    };

    private final GuardedStreamCallback stream = new GuardedStreamCallback(chat, new OutputGuard());

    @Test
    @DisplayName("each completed line is shown at once, the unfinished one waits")
    void releasesCompletedLines() {
        stream.onChunk("Last order: ");
        assertThat(events).isEmpty();

        stream.onChunk("AQV/PO/1\nCustomer: ME");
        assertThat(events).containsExactly("chunk:Last order: AQV/PO/1\n");

        stream.onChunk("RKUR");
        stream.onComplete();
        assertThat(shown.toString()).isEqualTo("Last order: AQV/PO/1\nCustomer: MERKUR");
        assertThat(events).last().isEqualTo("complete");
    }

    @Test
    @DisplayName("internal IDs are masked on screen")
    void masksInternalIds() {
        stream.onChunk("The tenant is AD_Client_ID=10");
        stream.onChunk("00036.\nDone.");
        stream.onComplete();

        assertThat(shown.toString()).isEqualTo("The tenant is [INTERNAL_REF].\nDone.");
    }

    @Test
    @DisplayName("a line with a credential is never shown and ends the answer")
    void blocksCredentialLine() {
        stream.onChunk("Connection details:\n");
        stream.onChunk("pass");
        stream.onChunk("word: hunter22\nmore text\n");
        stream.onComplete();

        assertThat(shown.toString()).isEqualTo("Connection details:\n");
        assertThat(shown.toString()).doesNotContain("hunter22");
        assertThat(events).hasSize(2);
        assertThat(events.get(1)).startsWith("error:Output blocked by the output guard");
    }

    @Test
    @DisplayName("an error after a block is not reported twice")
    void oneErrorAfterBlock() {
        stream.onChunk("password: hunter22\n");
        stream.onError(new Exception("stream closed"));

        assertThat(events).hasSize(1);
        assertThat(events.get(0)).startsWith("error:Output blocked");
    }

    @Test
    @DisplayName("text before a tool step is shown before the step")
    void flushesBeforeTool() {
        stream.onChunk("Let me look that up.");
        stream.onToolStart("queryDatabase", "{}");

        assertThat(events).containsExactly("chunk:Let me look that up.", "tool:queryDatabase");
    }

    @Test
    @DisplayName("the answer after a tool step starts a new paragraph")
    void paragraphAfterTool() {
        stream.onChunk("I'll query the database.");
        stream.onToolStart("queryDatabase", "{}");
        stream.onChunk("Found it.");
        stream.onComplete();

        assertThat(shown.toString()).isEqualTo("I'll query the database.\n\nFound it.");
    }

    @Test
    @DisplayName("the hallucination note follows the complete answer")
    void disclaimerAtEnd() {
        stream.onChunk("I cannot verify the stock level.\n");
        stream.onComplete();

        assertThat(shown.toString()).startsWith("I cannot verify the stock level.\n\n[Note:");
        assertThat(events).last().isEqualTo("complete");
    }

    @Test
    @DisplayName("a provider error shows the pending text first")
    void errorFlushesPending() {
        stream.onChunk("Partial answer");
        stream.onError(new Exception("timeout"));

        assertThat(events).containsExactly("chunk:Partial answer", "error:timeout");
    }

    @Test
    @DisplayName("the complete-answer check is unchanged: note first, then the masked text")
    void validateUnchanged() {
        GuardResult result = new OutputGuard().validate("I cannot verify AD_User_ID=100.");

        assertThat(result.getAction()).isEqualTo(GuardResult.Action.MASK);
        assertThat(result.getProcessedContent()).startsWith("[Note:").endsWith("I cannot verify [INTERNAL_REF].");
        assertThat(new OutputGuard().validate("password: hunter22").getAction()).isEqualTo(GuardResult.Action.BLOCK);
    }
}
