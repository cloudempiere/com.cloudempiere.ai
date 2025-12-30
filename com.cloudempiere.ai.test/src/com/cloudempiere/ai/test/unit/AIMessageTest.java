package com.cloudempiere.ai.test.unit;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cloudempiere.ai.provider.dto.AIFunctionCall;
import com.cloudempiere.ai.provider.dto.AIMessage;
import com.cloudempiere.ai.test.categories.UnitTest;

/**
 * Unit tests for {@link AIMessage} class.
 *
 * <p>Tests the message DTO and function call handling
 * without any external dependencies.
 *
 * @author Cloudempiere
 */
@UnitTest
@DisplayName("AIMessage Unit Tests")
class AIMessageTest {

    @Test
    @DisplayName("Constructor should set role and content")
    void constructor_shouldSetRoleAndContent() {
        AIMessage message = new AIMessage("user", "Hello, AI!");

        assertThat(message.getRole()).isEqualTo("user");
        assertThat(message.getContent()).isEqualTo("Hello, AI!");
    }

    @Test
    @DisplayName("User message should have user role")
    void userMessage_shouldHaveUserRole() {
        AIMessage message = new AIMessage("user", "What is iDempiere?");

        assertThat(message.getRole()).isEqualTo("user");
    }

    @Test
    @DisplayName("Assistant message should have assistant role")
    void assistantMessage_shouldHaveAssistantRole() {
        AIMessage message = new AIMessage("assistant", "iDempiere is an open-source ERP system.");

        assertThat(message.getRole()).isEqualTo("assistant");
    }

    @Test
    @DisplayName("System message should have system role")
    void systemMessage_shouldHaveSystemRole() {
        AIMessage message = new AIMessage("system", "You are an ERP expert.");

        assertThat(message.getRole()).isEqualTo("system");
    }

    @Test
    @DisplayName("Function message should have function role")
    void functionMessage_shouldHaveFunctionRole() {
        AIMessage message = new AIMessage("function", "{\"result\": \"success\"}");

        assertThat(message.getRole()).isEqualTo("function");
    }

    @Test
    @DisplayName("setName should store function name")
    void setName_shouldStoreFunctionName() {
        AIMessage message = new AIMessage("function", "{\"data\": []}");
        message.setName("get_orders");

        assertThat(message.getName()).isEqualTo("get_orders");
    }

    @Test
    @DisplayName("setFunctionCall should store single function call")
    void setFunctionCall_shouldStoreSingleFunctionCall() {
        AIMessage message = new AIMessage("assistant", "");
        AIFunctionCall functionCall = new AIFunctionCall();
        functionCall.setName("search_products");
        functionCall.setArguments("{\"query\": \"laptop\"}");

        message.setFunctionCall(functionCall);

        assertThat(message.getFunctionCall()).isNotNull();
        assertThat(message.getFunctionCall().getName()).isEqualTo("search_products");
        assertThat(message.getFunctionCall().getArguments()).isEqualTo("{\"query\": \"laptop\"}");
    }

    @Test
    @DisplayName("setFunctionCalls should store multiple function calls")
    void setFunctionCalls_shouldStoreMultipleFunctionCalls() {
        AIMessage message = new AIMessage("assistant", "");

        AIFunctionCall call1 = new AIFunctionCall();
        call1.setName("get_order");
        call1.setArguments("{\"orderId\": 123}");

        AIFunctionCall call2 = new AIFunctionCall();
        call2.setName("get_customer");
        call2.setArguments("{\"customerId\": 456}");

        message.setFunctionCalls(Arrays.asList(call1, call2));

        assertThat(message.getFunctionCalls())
            .hasSize(2)
            .extracting(AIFunctionCall::getName)
            .containsExactly("get_order", "get_customer");
    }

    @Test
    @DisplayName("setFunctionName should store function name for result messages")
    void setFunctionName_shouldStoreFunctionNameForResultMessages() {
        AIMessage message = new AIMessage("function", "{\"status\": \"completed\"}");
        message.setFunctionName("process_order");

        assertThat(message.getFunctionName()).isEqualTo("process_order");
    }

    @Test
    @DisplayName("setRole should update role")
    void setRole_shouldUpdateRole() {
        AIMessage message = new AIMessage("user", "Hello");

        message.setRole("assistant");

        assertThat(message.getRole()).isEqualTo("assistant");
    }

    @Test
    @DisplayName("setContent should update content")
    void setContent_shouldUpdateContent() {
        AIMessage message = new AIMessage("user", "Original message");

        message.setContent("Updated message");

        assertThat(message.getContent()).isEqualTo("Updated message");
    }

    @Test
    @DisplayName("New message should have null optional fields")
    void newMessage_shouldHaveNullOptionalFields() {
        AIMessage message = new AIMessage("user", "Hello");

        assertThat(message.getName()).isNull();
        assertThat(message.getFunctionCall()).isNull();
        assertThat(message.getFunctionCalls()).isNull();
        assertThat(message.getFunctionName()).isNull();
    }

    @Test
    @DisplayName("Message content can be empty string")
    void messageContent_canBeEmptyString() {
        AIMessage message = new AIMessage("assistant", "");

        assertThat(message.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Message content can contain multiline text")
    void messageContent_canContainMultilineText() {
        String multilineContent = "Line 1\nLine 2\nLine 3";
        AIMessage message = new AIMessage("user", multilineContent);

        assertThat(message.getContent())
            .isEqualTo(multilineContent)
            .contains("\n");
    }

    @Test
    @DisplayName("Message content can contain special characters")
    void messageContent_canContainSpecialCharacters() {
        String specialContent = "Price: $100.00 (€85.50) - 10% discount!";
        AIMessage message = new AIMessage("user", specialContent);

        assertThat(message.getContent()).isEqualTo(specialContent);
    }

    @Test
    @DisplayName("Message content can contain JSON")
    void messageContent_canContainJson() {
        String jsonContent = "{\"orderId\": 123, \"items\": [{\"product\": \"A\", \"qty\": 5}]}";
        AIMessage message = new AIMessage("function", jsonContent);

        assertThat(message.getContent()).isEqualTo(jsonContent);
    }
}
