package com.expenseassistant.conversation.service;

import com.expenseassistant.common.exception.ResourceNotFoundException;
import com.expenseassistant.common.response.PageResponse;
import com.expenseassistant.conversation.dto.ConversationDetailResponse;
import com.expenseassistant.conversation.dto.ConversationSummaryResponse;
import com.expenseassistant.conversation.dto.MessageResponse;
import com.expenseassistant.conversation.entity.ChatMessage;
import com.expenseassistant.conversation.entity.ChatMessageMetadata;
import com.expenseassistant.conversation.entity.Conversation;
import com.expenseassistant.conversation.entity.MessageRole;
import com.expenseassistant.conversation.mapper.ConversationMapper;
import com.expenseassistant.conversation.repository.ChatMessageRepository;
import com.expenseassistant.conversation.repository.ConversationRepository;
import com.expenseassistant.user.entity.User;
import com.expenseassistant.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationService {

    public static final String PLACEHOLDER_TITLE = "New chat";
    public static final String NOT_FOUND = "Conversation not found";

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ConversationMapper conversationMapper;

    public ConversationService(
            ConversationRepository conversationRepository,
            ChatMessageRepository chatMessageRepository,
            UserRepository userRepository,
            ConversationMapper conversationMapper) {
        this.conversationRepository = conversationRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.conversationMapper = conversationMapper;
    }

    @Transactional
    public Conversation createForUser(Long userId, String title) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return conversationRepository.save(new Conversation(user, title));
    }

    @Transactional(readOnly = true)
    public Conversation getOwned(Long userId, Long id) {
        return conversationRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }

    @Transactional
    public Conversation getOwnedOrCreate(Long userId, Long conversationId) {
        if (conversationId == null) {
            return createForUser(userId, PLACEHOLDER_TITLE);
        }
        return getOwned(userId, conversationId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MessageResponse addUserMessageInNewTransaction(Long userId, Long conversationId, String content) {
        return addMessage(userId, conversationId, MessageRole.USER, content, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MessageResponse addAssistantMessageInNewTransaction(
            Long userId, Long conversationId, String content, ChatMessageMetadata metadata) {
        return addMessage(userId, conversationId, MessageRole.ASSISTANT, content, metadata);
    }

    @Transactional
    public MessageResponse addMessage(
            Long userId, Long conversationId, MessageRole role, String content, ChatMessageMetadata metadata) {
        Conversation conversation = getOwned(userId, conversationId);
        ChatMessage message = chatMessageRepository.save(new ChatMessage(conversation, role, content, metadata));
        conversationRepository.save(conversation);
        return conversationMapper.toMessageResponse(message);
    }

    @Transactional
    public void updateTitleIfPlaceholder(Long userId, Long conversationId, String title) {
        Conversation conversation = getOwned(userId, conversationId);
        if (PLACEHOLDER_TITLE.equals(conversation.getTitle())) {
            conversation.setTitle(title);
            conversationRepository.save(conversation);
        }
    }

    @Transactional(readOnly = true)
    public boolean isFirstMessage(Long conversationId) {
        return chatMessageRepository.countByConversationId(conversationId) == 0;
    }

    @Transactional(readOnly = true)
    public PageResponse<ConversationSummaryResponse> list(Long userId, int page, int size) {
        Page<Conversation> conversationPage =
                conversationRepository.findByUserIdOrderByUpdatedAtDesc(
                        userId, ConversationPageSupport.listPageable(page, size));
        List<Long> ids = conversationPage.getContent().stream()
                .map(Conversation::getId)
                .toList();
        Map<Long, String> previews = loadLastMessagePreviews(ids);
        List<ConversationSummaryResponse> content = conversationPage.getContent().stream()
                .map(c -> conversationMapper.toSummary(c, previews.getOrDefault(c.getId(), "")))
                .toList();
        return new PageResponse<>(
                content,
                conversationPage.getNumber(),
                conversationPage.getSize(),
                conversationPage.getTotalElements(),
                conversationPage.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ConversationDetailResponse getDetail(Long userId, Long id) {
        Conversation conversation = getOwned(userId, id);
        long messageCount = chatMessageRepository.countByConversationId(id);
        return conversationMapper.toDetail(conversation, messageCount);
    }

    @Transactional(readOnly = true)
    public PageResponse<MessageResponse> messages(Long userId, Long conversationId, int page, int size) {
        getOwned(userId, conversationId);
        Page<ChatMessage> messagePage = chatMessageRepository.findByConversationIdOrderByCreatedAtAscIdAsc(
                conversationId, ConversationPageSupport.messagesPageable(page, size));
        List<MessageResponse> content = messagePage.getContent().stream()
                .map(conversationMapper::toMessageResponse)
                .toList();
        return new PageResponse<>(
                content,
                messagePage.getNumber(),
                messagePage.getSize(),
                messagePage.getTotalElements(),
                messagePage.getTotalPages());
    }

    private Map<Long, String> loadLastMessagePreviews(List<Long> conversationIds) {
        if (conversationIds.isEmpty()) {
            return Map.of();
        }
        return chatMessageRepository.findLatestByConversationIds(conversationIds).stream()
                .collect(Collectors.toMap(
                        m -> m.getConversation().getId(),
                        m -> ConversationMapper.previewContent(m.getContent()),
                        (a, b) -> a));
    }
}