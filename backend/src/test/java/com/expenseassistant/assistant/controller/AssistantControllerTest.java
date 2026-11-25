package com.expenseassistant.assistant.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expenseassistant.assistant.dto.ActionResultResponse;
import com.expenseassistant.assistant.dto.ActionResultStatus;
import com.expenseassistant.assistant.dto.ChatRequest;
import com.expenseassistant.assistant.dto.ChatResponse;
import com.expenseassistant.conversation.dto.MessageResponse;
import com.expenseassistant.conversation.entity.MessageRole;
import com.expenseassistant.assistant.service.AssistantActionService;
import com.expenseassistant.common.exception.PendingActionExpiredException;
import com.expenseassistant.common.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.expenseassistant.assistant.dto.ExpenseDraft;
import com.expenseassistant.assistant.dto.ExpenseExtractionResponse;
import com.expenseassistant.assistant.service.AssistantService;
import com.expenseassistant.assistant.service.ExpenseExtractionService;
import com.expenseassistant.expense.entity.ExpenseCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.expenseassistant.common.exception.AiUnavailableException;
import com.expenseassistant.common.exception.GlobalExceptionHandler;
import com.expenseassistant.security.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AssistantController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.ai.model.chat=google-genai")
class AssistantControllerTest {

    private static final String CHAT = "/api/v1/assistant/chat";
    private static final String EXTRACT = "/api/v1/assistant/extract";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssistantService assistantService;

    @MockitoBean
    private AssistantActionService assistantActionService;

    @MockitoBean
    private ExpenseExtractionService expenseExtractionService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void chat_returns200WithReply() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        Instant now = Instant.parse("2026-11-23T10:00:00Z");
        when(assistantService.chat(eq(1L), any(ChatRequest.class)))
                .thenReturn(new ChatResponse(
                        1L,
                        "Hi",
                        new MessageResponse(1L, MessageRole.USER, "Hi", now, List.of()),
                        new MessageResponse(2L, MessageRole.ASSISTANT, "Hello from assistant", now, List.of()),
                        null));

        mockMvc.perform(post(CHAT)
                        .contentType(APPLICATION_JSON)
                        .content("{\"message\":\"Hi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assistantMessage.content").value("Hello from assistant"));
    }

    @Test
    void chat_blankMessage_returns400() throws Exception {
        mockMvc.perform(post(CHAT)
                        .contentType(APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void extract_returns200WithDraftAndMissing() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        ExpenseExtractionResponse body =
                new ExpenseExtractionResponse(
                        new ExpenseDraft(new BigDecimal("500.00"), ExpenseCategory.FOOD, "Dinner", LocalDate.of(2026, 11, 24)),
                        List.of(),
                        null);
        when(expenseExtractionService.extract(eq(1L), eq("I spent 500 on dinner"))).thenReturn(body);

        mockMvc.perform(post(EXTRACT)
                        .contentType(APPLICATION_JSON)
                        .content("{\"message\":\"I spent 500 on dinner\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.draft.amount").value(500.00))
                .andExpect(jsonPath("$.draft.category").value("FOOD"))
                .andExpect(jsonPath("$.missing").isEmpty());
    }

    @Test
    void extract_blankMessage_returns400() throws Exception {
        mockMvc.perform(post(EXTRACT)
                        .contentType(APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void extract_aiUnavailable_returns503() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        when(expenseExtractionService.extract(eq(1L), any())).thenThrow(new AiUnavailableException());

        mockMvc.perform(post(EXTRACT)
                        .contentType(APPLICATION_JSON)
                        .content("{\"message\":\"spent 500\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("AI_UNAVAILABLE"));
    }

    @Test
    void confirmAction_returns200() throws Exception {
        UUID actionId = UUID.randomUUID();
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        Instant now = Instant.parse("2026-11-23T10:00:00Z");
        when(assistantActionService.confirm(eq(1L), eq(actionId)))
                .thenReturn(new ActionResultResponse(
                        actionId,
                        ActionResultStatus.CONFIRMED,
                        new MessageResponse(
                                5L, MessageRole.ASSISTANT, "Deleted: test", now, List.of())));

        mockMvc.perform(post("/api/v1/assistant/actions/" + actionId + "/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.message.content").value("Deleted: test"));
    }

    @Test
    void cancelAction_returns200() throws Exception {
        UUID actionId = UUID.randomUUID();
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        Instant now = Instant.parse("2026-11-23T10:00:00Z");
        when(assistantActionService.cancel(eq(1L), eq(actionId)))
                .thenReturn(new ActionResultResponse(
                        actionId,
                        ActionResultStatus.CANCELLED,
                        new MessageResponse(
                                6L,
                                MessageRole.ASSISTANT,
                                "Okay, I did not change anything.",
                                now,
                                List.of())));

        mockMvc.perform(post("/api/v1/assistant/actions/" + actionId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void confirmAction_notFound_returns404() throws Exception {
        UUID actionId = UUID.randomUUID();
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        when(assistantActionService.confirm(eq(1L), eq(actionId)))
                .thenThrow(new ResourceNotFoundException("Pending action not found"));

        mockMvc.perform(post("/api/v1/assistant/actions/" + actionId + "/confirm"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void confirmAction_expired_returns410() throws Exception {
        UUID actionId = UUID.randomUUID();
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        when(assistantActionService.confirm(eq(1L), eq(actionId))).thenThrow(new PendingActionExpiredException());

        mockMvc.perform(post("/api/v1/assistant/actions/" + actionId + "/confirm"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error").value("ACTION_EXPIRED"));
    }

    @Test
    void confirmAction_invalidUuid_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/assistant/actions/not-a-uuid/confirm"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void chat_aiUnavailable_returns503() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        when(assistantService.chat(eq(1L), any(ChatRequest.class))).thenThrow(new AiUnavailableException());

        mockMvc.perform(post(CHAT)
                        .contentType(APPLICATION_JSON)
                        .content("{\"message\":\"Hi\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("AI_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value(AiUnavailableException.USER_MESSAGE));
    }
}