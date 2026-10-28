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
package com.cloudempiere.ai.test.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.provider.langchain4j.ThreadAwareChatMemory;
import com.cloudempiere.ai.test.categories.UnitTest;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;

/**
 * The memory window sent to Bedrock must never hold a {@code tool_result} without its {@code tool_use}, and must start
 * with a user message. A plain cut at {@code size - maxMessages} broke both and Bedrock answered
 * "unexpected tool_use_id found in tool_result blocks" (CLD-2425).
 */
@UnitTest
@DisplayName("ThreadAwareChatMemory.trimWindow keeps tool calls and their results together")
class ThreadAwareChatMemoryTrimTest {

    private static final int MAX = 20;

    private int toolSeq;

    @Test
    @DisplayName("Should return the messages unchanged when under the limit")
    void shouldKeepWindowUnderLimit() {
        List<ChatMessage> messages = textTurns(5);

        List<ChatMessage> trimmed = ThreadAwareChatMemory.trimWindow(messages, MAX);

        assertThat(trimmed).containsExactlyElementsOf(messages);
    }

    @Test
    @DisplayName("Should keep the most recent text turns and start with a user message")
    void shouldKeepMostRecentTextTurns() {
        List<ChatMessage> messages = textTurns(15);

        List<ChatMessage> trimmed = ThreadAwareChatMemory.trimWindow(messages, MAX);

        assertThat(trimmed).containsExactlyElementsOf(messages.subList(10, 30));
        assertThat(trimmed.get(0)).isInstanceOf(UserMessage.class);
    }

    @Test
    @DisplayName("Should not start with an assistant message when the cut lands on one")
    void shouldNotStartWithAssistantMessage() {
        List<ChatMessage> messages = new ArrayList<>(textTurns(10));
        messages.add(UserMessage.from("last question"));

        // 21 messages: the plain cut would start at index 1, an assistant answer
        List<ChatMessage> trimmed = ThreadAwareChatMemory.trimWindow(messages, MAX);

        assertThat(trimmed.get(0)).isInstanceOf(UserMessage.class);
        assertThat(trimmed).hasSizeLessThanOrEqualTo(MAX);
        assertThat(trimmed.get(trimmed.size() - 1)).isSameAs(messages.get(messages.size() - 1));
    }

    @Test
    @DisplayName("Should not keep a tool result whose parallel tool call was cut off")
    void shouldNotOrphanParallelToolResults() {
        List<ChatMessage> messages = new ArrayList<>(textTurns(8));
        messages.add(UserMessage.from("stock and open orders for product X?"));
        messages.addAll(toolRound(2));
        messages.add(AiMessage.from("answer"));
        messages.addAll(textTurns(3));

        // 16 + 1 + 3 + 1 + 6 = 27 messages: with max 8 the plain cut starts on the second tool result
        int max = 8;
        assertThat(messages.get(messages.size() - max)).isInstanceOf(ToolExecutionResultMessage.class);
        List<ChatMessage> trimmed = ThreadAwareChatMemory.trimWindow(messages, max);

        assertThat(trimmed.get(0)).isInstanceOf(UserMessage.class);
        assertThat(trimmed).hasSizeLessThanOrEqualTo(max);
        assertToolCallsPaired(trimmed);
    }

    @Test
    @DisplayName("Should keep tool calls paired for every possible cut position")
    void shouldKeepToolCallsPairedAtEveryCut() {
        List<ChatMessage> messages = new ArrayList<>();
        for (int turn = 0; turn < 6; turn++) {
            messages.add(UserMessage.from("question " + turn));
            for (int round = 0; round <= turn % 3; round++) {
                messages.addAll(toolRound(1 + round));
            }
            messages.add(AiMessage.from("answer " + turn));
        }

        for (int max = 1; max <= messages.size(); max++) {
            List<ChatMessage> trimmed = ThreadAwareChatMemory.trimWindow(messages, max);

            assertThat(trimmed.get(0)).as("first message for max %d", max).isInstanceOf(UserMessage.class);
            assertToolCallsPaired(trimmed);
            assertThat(trimmed.get(trimmed.size() - 1)).isSameAs(messages.get(messages.size() - 1));
        }
    }

    @Test
    @DisplayName("Should keep the whole current turn when its tool rounds exceed the limit")
    void shouldKeepCurrentTurnLargerThanWindow() {
        List<ChatMessage> messages = new ArrayList<>(textTurns(3));
        UserMessage question = UserMessage.from("analyse all open orders");
        messages.add(question);
        for (int round = 0; round < 12; round++) {
            messages.addAll(toolRound(1));
        }

        List<ChatMessage> trimmed = ThreadAwareChatMemory.trimWindow(messages, MAX);

        // the turn alone is 25 messages: keep it whole rather than lose the question
        assertThat(trimmed.get(0)).isSameAs(question);
        assertThat(trimmed).hasSize(25);
        assertToolCallsPaired(trimmed);
    }

    @Test
    @DisplayName("Should keep the system message and not count it towards the limit")
    void shouldKeepSystemMessage() {
        SystemMessage system = SystemMessage.from("You are the ERP assistant.");
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(system);
        messages.addAll(textTurns(15));

        List<ChatMessage> trimmed = ThreadAwareChatMemory.trimWindow(messages, MAX);

        assertThat(trimmed.get(0)).isSameAs(system);
        assertThat(trimmed).hasSize(MAX + 1);
        assertThat(trimmed.get(1)).isInstanceOf(UserMessage.class);
    }

    @Test
    @DisplayName("Should drop leading orphan tool results when there is no user message")
    void shouldDropOrphanResultsWithoutUserMessage() {
        List<ChatMessage> messages = new ArrayList<>();
        messages.addAll(toolRound(1));
        messages.addAll(toolRound(1));

        // max 3 cuts the first tool request and keeps its result
        List<ChatMessage> trimmed = ThreadAwareChatMemory.trimWindow(messages, 3);

        assertThat(trimmed).containsExactlyElementsOf(messages.subList(2, 4));
        assertToolCallsPaired(trimmed);
    }

    @Test
    @DisplayName("Should return a mutable copy")
    void shouldReturnMutableCopy() {
        List<ChatMessage> messages = textTurns(2);

        List<ChatMessage> trimmed = ThreadAwareChatMemory.trimWindow(messages, MAX);
        trimmed.add(UserMessage.from("next"));

        assertThat(messages).hasSize(4);
    }

    // ------------------------------------------------------------------------

    private static List<ChatMessage> textTurns(int count) {
        List<ChatMessage> messages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            messages.add(UserMessage.from("question " + i));
            messages.add(AiMessage.from("answer " + i));
        }
        return messages;
    }

    /** One assistant message with {@code parallel} tool requests, followed by one result per request. */
    private List<ChatMessage> toolRound(int parallel) {
        List<ToolExecutionRequest> requests = new ArrayList<>();
        for (int i = 0; i < parallel; i++) {
            requests.add(ToolExecutionRequest.builder()
                .id("toolu_" + (++toolSeq))
                .name("executeQuery")
                .arguments("{}")
                .build());
        }
        List<ChatMessage> round = new ArrayList<>();
        round.add(AiMessage.from(requests));
        for (ToolExecutionRequest request : requests) {
            round.add(ToolExecutionResultMessage.from(request, "[]"));
        }
        return round;
    }

    /**
     * Same rule as Bedrock: every tool result answers a request of the closest preceding assistant message, and every
     * request of an assistant message is answered before the next non-tool message.
     */
    private static void assertToolCallsPaired(List<ChatMessage> messages) {
        Set<String> open = new HashSet<>();
        for (ChatMessage message : messages) {
            if (message instanceof ToolExecutionResultMessage) {
                String id = ((ToolExecutionResultMessage) message).id();
                assertThat(open).as("tool result %s has its request right before it", id).contains(id);
                open.remove(id);
                continue;
            }
            assertThat(open).as("tool requests answered before %s", message.type()).isEmpty();
            if (message instanceof AiMessage && ((AiMessage) message).hasToolExecutionRequests()) {
                for (ToolExecutionRequest request : ((AiMessage) message).toolExecutionRequests()) {
                    open.add(request.id());
                }
            }
        }
        assertThat(open).as("tool requests answered at the end of the window").isEmpty();
    }
}
