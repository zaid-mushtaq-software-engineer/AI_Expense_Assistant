package com.expenseassistant.conversation.controller;

import com.expenseassistant.common.response.PageResponse;
import com.expenseassistant.conversation.dto.ConversationDetailResponse;
import com.expenseassistant.conversation.dto.ConversationSummaryResponse;
import com.expenseassistant.conversation.dto.MessageResponse;
import com.expenseassistant.conversation.service.ConversationService;
import com.expenseassistant.security.CurrentUserProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final CurrentUserProvider currentUserProvider;

    public ConversationController(
            ConversationService conversationService, CurrentUserProvider currentUserProvider) {
        this.conversationService = conversationService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public PageResponse<ConversationSummaryResponse> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Long userId = currentUserProvider.requireUserId();
        return conversationService.list(userId, page, size);
    }

    @GetMapping("/{id}")
    public ConversationDetailResponse get(@PathVariable Long id) {
        Long userId = currentUserProvider.requireUserId();
        return conversationService.getDetail(userId, id);
    }

    @GetMapping("/{id}/messages")
    public PageResponse<MessageResponse> messages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long userId = currentUserProvider.requireUserId();
        return conversationService.messages(userId, id, page, size);
    }
}