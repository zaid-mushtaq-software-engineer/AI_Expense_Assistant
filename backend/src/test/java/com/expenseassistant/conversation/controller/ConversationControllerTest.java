package com.expenseassistant.conversation.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expenseassistant.common.exception.GlobalExceptionHandler;
import com.expenseassistant.common.exception.ResourceNotFoundException;
import com.expenseassistant.common.response.PageResponse;
import com.expenseassistant.conversation.dto.ConversationDetailResponse;
import com.expenseassistant.conversation.service.ConversationService;
import com.expenseassistant.security.CurrentUserProvider;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ConversationController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc
class ConversationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConversationService conversationService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void list_returns200() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        when(conversationService.list(1L, 0, 20)).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void get_foreignId_returns404() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        when(conversationService.getDetail(eq(1L), eq(99L)))
                .thenThrow(new ResourceNotFoundException("Conversation not found"));

        mockMvc.perform(get("/api/v1/conversations/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Conversation not found"));
    }

    @Test
    void get_returns200() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        Instant now = Instant.parse("2026-11-23T10:00:00Z");
        when(conversationService.getDetail(1L, 5L))
                .thenReturn(new ConversationDetailResponse(5L, "Dinner", now, now, 3L));

        mockMvc.perform(get("/api/v1/conversations/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.messageCount").value(3));
    }
}