package com.expenseassistant.assistant.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.expenseassistant.assistant.dto.ChatRequest;
import com.expenseassistant.assistant.prompt.SystemPromptProvider;
import com.expenseassistant.assistant.tools.AnalyticsTools;
import com.expenseassistant.assistant.tools.ExpenseTools;
import com.expenseassistant.common.exception.AiUnavailableException;
import com.expenseassistant.conversation.dto.MessageResponse;
import com.expenseassistant.conversation.entity.Conversation;
import com.expenseassistant.conversation.entity.MessageRole;
import com.expenseassistant.conversation.service.ConversationService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class AssistantServiceTest {

    @Test
    void chat_missingApiKey_throwsAiUnavailable() {
        ChatClient chatClient = mock(ChatClient.class);
        SystemPromptProvider promptProvider = mock(SystemPromptProvider.class);
        AnalyticsTools analyticsTools = mock(AnalyticsTools.class);
        ExpenseTools expenseTools = mock(ExpenseTools.class);
        ChatTurnContext chatTurnContext = new ChatTurnContext();
        ConversationService conversationService = mock(ConversationService.class);

        Conversation conversation = mock(Conversation.class);
        when(conversation.getId()).thenReturn(10L);
        when(conversation.getTitle()).thenReturn(ConversationService.PLACEHOLDER_TITLE);
        when(conversationService.getOwnedOrCreate(eq(1L), any())).thenReturn(conversation);
        when(conversationService.isFirstMessage(10L)).thenReturn(true);
        when(conversationService.addUserMessageInNewTransaction(eq(1L), eq(10L), eq("Hello")))
                .thenReturn(new MessageResponse(1L, MessageRole.USER, "Hello", Instant.now(), List.of()));

        AssistantService service = new AssistantService(
                chatClient, promptProvider, analyticsTools, expenseTools, chatTurnContext, conversationService, "");

        assertThatThrownBy(() -> service.chat(1L, new ChatRequest(null, "Hello")))
                .isInstanceOf(AiUnavailableException.class)
                .hasMessage(AiUnavailableException.USER_MESSAGE);
    }
}