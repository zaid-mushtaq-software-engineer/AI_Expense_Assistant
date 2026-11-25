package com.expenseassistant.assistant.service;

import com.expenseassistant.assistant.dto.ChatRequest;
import com.expenseassistant.assistant.dto.ChatResponse;
import com.expenseassistant.assistant.dto.PendingActionResponse;
import com.expenseassistant.assistant.prompt.SystemPromptProvider;
import com.expenseassistant.assistant.tools.AnalyticsTools;
import com.expenseassistant.assistant.tools.AssistantToolContextKeys;
import com.expenseassistant.assistant.tools.ExpenseTools;
import com.expenseassistant.common.exception.AiUnavailableException;
import com.expenseassistant.conversation.dto.MessageResponse;
import com.expenseassistant.conversation.entity.ChatMessageMetadata;
import com.expenseassistant.conversation.entity.Conversation;
import com.expenseassistant.conversation.mapper.ConversationMapper;
import com.expenseassistant.conversation.service.ConversationService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "google-genai")
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final ChatClient chatClient;
    private final SystemPromptProvider systemPromptProvider;
    private final AnalyticsTools analyticsTools;
    private final ExpenseTools expenseTools;
    private final ChatTurnContext chatTurnContext;
    private final ConversationService conversationService;
    private final String apiKey;

    public AssistantService(
            ChatClient chatClient,
            SystemPromptProvider systemPromptProvider,
            AnalyticsTools analyticsTools,
            ExpenseTools expenseTools,
            ChatTurnContext chatTurnContext,
            ConversationService conversationService,
            @Value("${spring.ai.google.genai.api-key:}") String apiKey) {
        this.chatClient = chatClient;
        this.systemPromptProvider = systemPromptProvider;
        this.analyticsTools = analyticsTools;
        this.expenseTools = expenseTools;
        this.chatTurnContext = chatTurnContext;
        this.conversationService = conversationService;
        this.apiKey = apiKey;
    }

    public ChatResponse chat(Long userId, ChatRequest request) {
        long started = System.nanoTime();
        Conversation conversation = conversationService.getOwnedOrCreate(userId, request.conversationId());
        Long conversationId = conversation.getId();
        boolean firstMessage = conversationService.isFirstMessage(conversationId);
        String conversationTitle = conversation.getTitle();

        MessageResponse userMessage =
                conversationService.addUserMessageInNewTransaction(userId, conversationId, request.message());

        if (firstMessage) {
            conversationTitle = ConversationTitleGenerator.fromFirstMessage(request.message());
            conversationService.updateTitleIfPlaceholder(userId, conversationId, conversationTitle);
        }

        try {
            requireApiKey();
            // chat_messages = durable UI history; SPRING_AI_CHAT_MEMORY = trimmed model context (advisor).
            String reply =
                    chatClient
                            .prompt()
                            .system(systemPromptProvider.render())
                            .user(request.message())
                            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, String.valueOf(conversationId)))
                            .tools(analyticsTools, expenseTools)
                            .toolContext(Map.of(
                                    AssistantToolContextKeys.USER_ID,
                                    userId,
                                    AssistantToolContextKeys.CONVERSATION_ID,
                                    conversationId))
                            .call()
                            .content();
            if (!StringUtils.hasText(reply)) {
                throw new AiUnavailableException();
            }
            PendingActionResponse pendingAction = chatTurnContext.getPendingAction().orElse(null);
            List<String> toolsUsed = chatTurnContext.getToolsUsed();
            String pendingActionId =
                    pendingAction != null ? pendingAction.actionId().toString() : null;
            ChatMessageMetadata metadata = ConversationMapper.buildAssistantMetadata(
                    toolsUsed, pendingActionId, chatTurnContext.getToolErrors());
            MessageResponse assistantMessage = conversationService.addAssistantMessageInNewTransaction(
                    userId, conversationId, reply.trim(), metadata);

            long durationMs = elapsedMs(started);
            log.info(
                    "Assistant chat userId={} conversationId={} tools={} outcome=success durationMs={}",
                    userId,
                    conversationId,
                    String.join(",", toolsUsed),
                    durationMs);
            return new ChatResponse(
                    conversationId, conversationTitle, userMessage, assistantMessage, pendingAction);
        } catch (AiUnavailableException e) {
            logUnavailable(userId, conversationId, started, e);
            throw e;
        } catch (Exception e) {
            AiUnavailableException wrapped = new AiUnavailableException(e);
            logUnavailable(userId, conversationId, started, wrapped);
            throw wrapped;
        }
    }

    private void requireApiKey() {
        if (!StringUtils.hasText(apiKey)) {
            throw new AiUnavailableException();
        }
    }

    private void logUnavailable(Long userId, Long conversationId, long started, AiUnavailableException ex) {
        long durationMs = elapsedMs(started);
        if (ex.getCause() != null) {
            log.warn(
                    "Assistant chat userId={} conversationId={} outcome=unavailable durationMs={}",
                    userId,
                    conversationId,
                    durationMs,
                    ex.getCause());
        } else {
            log.info(
                    "Assistant chat userId={} conversationId={} outcome=unavailable durationMs={}",
                    userId,
                    conversationId,
                    durationMs);
        }
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }
}