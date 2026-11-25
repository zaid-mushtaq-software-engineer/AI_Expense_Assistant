package com.expenseassistant.assistant.service;

import com.expenseassistant.assistant.dto.ExpenseDraft;
import com.expenseassistant.assistant.dto.ExpenseExtractionResponse;
import com.expenseassistant.assistant.prompt.ExtractionPromptProvider;
import com.expenseassistant.common.exception.AiUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "google-genai")
public class ExpenseExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExpenseExtractionService.class);

    private final ChatClient chatClient;
    private final ExtractionPromptProvider extractionPromptProvider;
    private final ExpenseDraftPostProcessor postProcessor;
    private final ExpenseDraftAnalyzer analyzer;
    private final String apiKey;

    public ExpenseExtractionService(
            ChatClient chatClient,
            ExtractionPromptProvider extractionPromptProvider,
            ExpenseDraftPostProcessor postProcessor,
            ExpenseDraftAnalyzer analyzer,
            @Value("${spring.ai.google.genai.api-key:}") String apiKey) {
        this.chatClient = chatClient;
        this.extractionPromptProvider = extractionPromptProvider;
        this.postProcessor = postProcessor;
        this.analyzer = analyzer;
        this.apiKey = apiKey;
    }

    public ExpenseExtractionResponse extract(Long userId, String text) {
        long started = System.nanoTime();
        try {
            requireApiKey();
            ExpenseDraft raw =
                    chatClient
                            .prompt()
                            .system(extractionPromptProvider.render())
                            .user(text)
                            .call()
                            .entity(ExpenseDraft.class);
            ExpenseDraft sanitized = postProcessor.sanitize(raw);
            ExpenseExtractionResponse response = analyzer.analyze(sanitized);
            long durationMs = elapsedMs(started);
            log.info("Assistant extract userId={} outcome=success durationMs={}", userId, durationMs);
            return response;
        } catch (AiUnavailableException e) {
            logUnavailable(userId, started, e);
            throw e;
        } catch (Exception e) {
            AiUnavailableException wrapped = new AiUnavailableException(e);
            logUnavailable(userId, started, wrapped);
            throw wrapped;
        }
    }

    private void requireApiKey() {
        if (!StringUtils.hasText(apiKey)) {
            throw new AiUnavailableException();
        }
    }

    private void logUnavailable(Long userId, long started, AiUnavailableException ex) {
        long durationMs = elapsedMs(started);
        if (ex.getCause() != null) {
            log.warn(
                    "Assistant extract userId={} outcome=unavailable durationMs={}",
                    userId,
                    durationMs,
                    ex.getCause());
        } else {
            log.info("Assistant extract userId={} outcome=unavailable durationMs={}", userId, durationMs);
        }
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000L;
    }
}