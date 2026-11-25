package com.expenseassistant.conversation.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class ConversationPageSupport {

    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;
    public static final int DEFAULT_LIST_SIZE = 20;

    private ConversationPageSupport() {}

    public static Pageable messagesPageable(int page, int size) {
        int effectiveSize = size <= 0 ? DEFAULT_SIZE : size;
        effectiveSize = Math.min(effectiveSize, MAX_SIZE);
        return PageRequest.of(Math.max(page, 0), effectiveSize, Sort.by("createdAt").ascending().and(Sort.by("id")));
    }

    public static Pageable listPageable(int page, int size) {
        int effectiveSize = size <= 0 ? DEFAULT_LIST_SIZE : size;
        effectiveSize = Math.min(effectiveSize, MAX_SIZE);
        return PageRequest.of(Math.max(page, 0), effectiveSize, Sort.by("updatedAt").descending());
    }
}