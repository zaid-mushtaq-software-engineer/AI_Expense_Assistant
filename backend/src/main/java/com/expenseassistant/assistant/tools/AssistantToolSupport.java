package com.expenseassistant.assistant.tools;

import static com.expenseassistant.assistant.tools.AssistantToolContextKeys.USER_ID;

import com.expenseassistant.assistant.tools.ToolResults.ErrorResult;
import com.expenseassistant.assistant.tools.ToolResults.ToolOutcome;
import com.expenseassistant.common.exception.ResourceNotFoundException;
import com.expenseassistant.common.exception.ValidationFailedException;
import com.expenseassistant.common.response.ErrorResponse.FieldError;
import com.expenseassistant.expense.entity.ExpenseCategory;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;
import com.expenseassistant.assistant.service.ChatTurnContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AssistantToolSupport {

    private static final Logger log = LoggerFactory.getLogger(AssistantToolSupport.class);

    private final ObjectProvider<ChatTurnContext> chatTurnContext;

    public AssistantToolSupport(ObjectProvider<ChatTurnContext> chatTurnContext) {
        this.chatTurnContext = chatTurnContext;
    }

    @FunctionalInterface
    public interface ToolAction<T> {
        T run(Long userId) throws Exception;
    }

    public <T, R> ToolOutcome<R> runTool(
            String toolName,
            ToolContext toolContext,
            ToolAction<T> action,
            java.util.function.Function<T, R> mapper,
            String internalErrorMessage) {
        Long userId = requireUserId(toolContext);
        if (userId == null) {
            log.debug("{} outcome=error reason=missing_user_context", toolName);
            recordTool(toolName, true);
            return ToolOutcome.err(
                    new ErrorResult("INTERNAL_ERROR", "Could not resolve the current user."));
        }
        try {
            T serviceResult = action.run(userId);
            log.debug("{} outcome=success", toolName);
            recordTool(toolName, false);
            return ToolOutcome.ok(mapper.apply(serviceResult));
        } catch (DateTimeParseException ex) {
            log.debug("{} outcome=error reason=invalid_date", toolName);
            recordTool(toolName, true);
            return ToolOutcome.err(new ErrorResult("INVALID_DATE", "Dates must be in yyyy-MM-dd format."));
        } catch (IllegalArgumentException ex) {
            log.debug("{} outcome=error reason=invalid_category", toolName);
            recordTool(toolName, true);
            return ToolOutcome.err(new ErrorResult("INVALID_CATEGORY", ex.getMessage()));
        } catch (ResourceNotFoundException ex) {
            log.debug("{} outcome=error reason=not_found", toolName);
            recordTool(toolName, true);
            return ToolOutcome.err(new ErrorResult("NOT_FOUND", ex.getMessage()));
        } catch (ValidationFailedException ex) {
            log.debug("{} outcome=error reason=validation", toolName);
            recordTool(toolName, true);
            return ToolOutcome.err(new ErrorResult("VALIDATION_ERROR", formatValidationMessage(ex)));
        } catch (Exception ex) {
            log.error("{} unexpected failure", toolName, ex);
            recordTool(toolName, true);
            return ToolOutcome.err(new ErrorResult("INTERNAL_ERROR", internalErrorMessage));
        }
    }

    private void recordTool(String toolName, boolean failed) {
        chatTurnContext.ifAvailable(ctx -> ctx.recordTool(toolName, failed));
    }

    public static Long conversationIdFrom(ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }
        Object raw = toolContext.getContext().get(AssistantToolContextKeys.CONVERSATION_ID);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Long id) {
            return id;
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public static Long requireUserId(ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }
        Object raw = toolContext.getContext().get(USER_ID);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Long id) {
            return id;
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public static LocalDate parseIsoDate(String value) {
        return LocalDate.parse(value);
    }

    public static LocalDate parseIsoDateOptional(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return LocalDate.parse(value.trim());
    }

    public static ExpenseCategory parseCategoryOptional(String category) {
        if (!StringUtils.hasText(category)) {
            return null;
        }
        try {
            return ExpenseCategory.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown category: " + category.trim());
        }
    }

    public static ExpenseCategory parseCategoryRequired(String category) {
        if (!StringUtils.hasText(category)) {
            throw new IllegalArgumentException("category is required");
        }
        return parseCategoryOptional(category);
    }

    public static String formatValidationMessage(ValidationFailedException ex) {
        if (ex.getFieldErrors().isEmpty()) {
            return ex.getMessage();
        }
        String fields = ex.getFieldErrors().stream()
                .map(AssistantToolSupport::formatFieldError)
                .collect(Collectors.joining("; "));
        return ex.getMessage() + " (" + fields + ")";
    }

    private static String formatFieldError(FieldError error) {
        return error.field() + ": " + error.message();
    }
}