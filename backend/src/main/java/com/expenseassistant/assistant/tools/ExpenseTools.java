package com.expenseassistant.assistant.tools;

import com.expenseassistant.assistant.dto.PendingActionResponse;
import com.expenseassistant.assistant.service.ChatTurnContext;
import com.expenseassistant.assistant.service.CreatedPendingAction;
import com.expenseassistant.assistant.service.DeleteExpensePayload;
import com.expenseassistant.assistant.service.PendingActionFormatter;
import com.expenseassistant.assistant.service.PendingActionService;
import com.expenseassistant.assistant.service.PendingActionType;
import com.expenseassistant.assistant.service.UpdateExpensePayload;
import com.expenseassistant.assistant.tools.ToolResults.ExpenseLine;
import com.expenseassistant.assistant.tools.ToolResults.ExpenseResult;
import com.expenseassistant.assistant.tools.ToolResults.ExpenseSearchResult;
import com.expenseassistant.assistant.tools.ToolResults.PendingResult;
import com.expenseassistant.assistant.tools.ToolResults.ToolOutcome;
import com.expenseassistant.common.exception.ValidationFailedException;
import com.expenseassistant.common.response.PageResponse;
import com.expenseassistant.expense.dto.CreateExpenseRequest;
import com.expenseassistant.expense.dto.ExpenseFilter;
import com.expenseassistant.expense.dto.ExpenseResponse;
import com.expenseassistant.expense.dto.UpdateExpenseRequest;
import com.expenseassistant.expense.entity.ExpenseCategory;
import com.expenseassistant.expense.service.ExpenseService;
import com.expenseassistant.expense.service.ExpenseWriteValidator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "google-genai")
public class ExpenseTools {

    private static final Logger log = LoggerFactory.getLogger(ExpenseTools.class);
    private static final int SEARCH_PAGE_SIZE = 20;

    private final ExpenseService expenseService;
    private final PendingActionService pendingActionService;
    private final PendingActionFormatter pendingActionFormatter;
    private final ExpenseWriteValidator expenseWriteValidator;
    private final ChatTurnContext chatTurnContext;
    private final AssistantToolSupport toolSupport;

    public ExpenseTools(
            ExpenseService expenseService,
            PendingActionService pendingActionService,
            PendingActionFormatter pendingActionFormatter,
            ExpenseWriteValidator expenseWriteValidator,
            ChatTurnContext chatTurnContext,
            AssistantToolSupport toolSupport) {
        this.expenseService = expenseService;
        this.pendingActionService = pendingActionService;
        this.pendingActionFormatter = pendingActionFormatter;
        this.expenseWriteValidator = expenseWriteValidator;
        this.chatTurnContext = chatTurnContext;
        this.toolSupport = toolSupport;
    }

    @Tool(
            description =
                    "Search the user's expenses with optional filters. Returns up to 20 matches plus total count. "
                            + "Dates are inclusive ISO yyyy-MM-dd when provided.")
    public ToolOutcome<ExpenseSearchResult> searchExpenses(
            @ToolParam(description = "Optional start date (yyyy-MM-dd)", required = false) String from,
            @ToolParam(description = "Optional end date (yyyy-MM-dd)", required = false) String to,
            @ToolParam(description = "Optional category filter", required = false) String category,
            @ToolParam(description = "Optional description search text", required = false) String text,
            ToolContext toolContext) {
        log.debug("searchExpenses from={} to={} category={}", from, to, category);
        return toolSupport.runTool(
                "searchExpenses",
                toolContext,
                userId -> {
                    LocalDate fromDate = AssistantToolSupport.parseIsoDateOptional(from);
                    LocalDate toDate = AssistantToolSupport.parseIsoDateOptional(to);
                    ExpenseCategory categoryFilter = AssistantToolSupport.parseCategoryOptional(category);
                    String q = StringUtils.hasText(text) ? text.trim() : null;
                    return expenseService.search(
                            userId, new ExpenseFilter(fromDate, toDate, categoryFilter, q), 0, SEARCH_PAGE_SIZE, null);
                },
                this::toSearchResult,
                "Could not search expenses.");
    }

    @Tool(
            description =
                    "Create a new expense for the current user. All parameters must be known from the user — "
                            + "do not guess. Category must be a valid enum name (e.g. FOOD). Date is yyyy-MM-dd.")
    public ToolOutcome<ExpenseResult> addExpense(
            @ToolParam(description = "Amount in INR, greater than 0") BigDecimal amount,
            @ToolParam(description = "Category enum name, e.g. FOOD") String category,
            @ToolParam(description = "Optional description", required = false) String description,
            @ToolParam(description = "Expense date yyyy-MM-dd") String expenseDate,
            ToolContext toolContext) {
        log.debug("addExpense category={} expenseDate={}", category, expenseDate);
        return toolSupport.runTool(
                "addExpense",
                toolContext,
                userId -> {
                    ExpenseCategory expenseCategory = AssistantToolSupport.parseCategoryRequired(category);
                    LocalDate date = AssistantToolSupport.parseIsoDate(expenseDate);
                    String desc = StringUtils.hasText(description) ? description.trim() : null;
                    CreateExpenseRequest request = new CreateExpenseRequest(amount, expenseCategory, desc, date);
                    return expenseService.create(userId, request);
                },
                this::toExpenseResult,
                "Could not add the expense.");
    }

    @Tool(
            description =
                    "Propose deleting one expense by id. Does NOT delete — the user must confirm via the app. "
                            + "Use searchExpenses first when the id is unknown or several expenses match.")
    public ToolOutcome<PendingResult> proposeExpenseDeletion(
            @ToolParam(description = "Id of the expense to delete") long expenseId, ToolContext toolContext) {
        log.debug("proposeExpenseDeletion expenseId={}", expenseId);
        return toolSupport.runTool(
                "proposeExpenseDeletion",
                toolContext,
                userId -> {
                    ExpenseResponse expense = expenseService.get(userId, expenseId);
                    String summary = pendingActionFormatter.formatDelete(expense);
                    return pendingActionService.create(
                            userId,
                            AssistantToolSupport.conversationIdFrom(toolContext),
                            PendingActionType.DELETE_EXPENSE,
                            new DeleteExpensePayload(expenseId),
                            summary);
                },
                this::toPendingResult,
                "Could not propose deletion.");
    }

    @Tool(
            description =
                    "Propose updating an expense by id. Pass only fields to change. Does NOT save — the user must "
                            + "confirm via the app. Use searchExpenses first when the id is unknown.")
    public ToolOutcome<PendingResult> proposeExpenseUpdate(
            @ToolParam(description = "Id of the expense to update") long expenseId,
            @ToolParam(description = "New amount in INR", required = false) BigDecimal amount,
            @ToolParam(description = "New category enum name", required = false) String category,
            @ToolParam(description = "New description", required = false) String description,
            @ToolParam(description = "New expense date yyyy-MM-dd", required = false) String expenseDate,
            ToolContext toolContext) {
        log.debug("proposeExpenseUpdate expenseId={}", expenseId);
        return toolSupport.runTool(
                "proposeExpenseUpdate",
                toolContext,
                userId -> {
                    ExpenseResponse current = expenseService.get(userId, expenseId);
                    UpdateExpenseRequest merged = mergeUpdate(current, amount, category, description, expenseDate);
                    if (isUnchanged(current, merged)) {
                        throw new ValidationFailedException("No changes to apply");
                    }
                    expenseWriteValidator.validate(merged);
                    String summary = pendingActionFormatter.formatUpdate(current, merged);
                    return pendingActionService.create(
                            userId,
                            AssistantToolSupport.conversationIdFrom(toolContext),
                            PendingActionType.UPDATE_EXPENSE,
                            new UpdateExpensePayload(expenseId, merged),
                            summary);
                },
                this::toPendingResult,
                "Could not propose update.");
    }

    private PendingResult toPendingResult(CreatedPendingAction created) {
        PendingActionResponse response =
                new PendingActionResponse(created.id(), created.type(), created.summary(), created.expiresAt());
        chatTurnContext.setPendingAction(response);
        return new PendingResult(
                created.id().toString(),
                created.type().name(),
                created.summary(),
                created.expiresAt().toString());
    }

    private UpdateExpenseRequest mergeUpdate(
            ExpenseResponse current,
            BigDecimal amount,
            String category,
            String description,
            String expenseDate) {
        BigDecimal newAmount = amount != null ? amount : current.amount();
        ExpenseCategory newCategory =
                StringUtils.hasText(category)
                        ? AssistantToolSupport.parseCategoryRequired(category)
                        : current.category();
        String newDescription =
                description != null
                        ? (StringUtils.hasText(description) ? description.trim() : null)
                        : current.description();
        LocalDate newDate =
                StringUtils.hasText(expenseDate)
                        ? AssistantToolSupport.parseIsoDate(expenseDate)
                        : current.expenseDate();
        return new UpdateExpenseRequest(newAmount, newCategory, newDescription, newDate);
    }

    private boolean isUnchanged(ExpenseResponse current, UpdateExpenseRequest merged) {
        return current.amount().compareTo(merged.amount()) == 0
                && current.category() == merged.category()
                && Objects.equals(normalizeDescription(current.description()), normalizeDescription(merged.description()))
                && current.expenseDate().equals(merged.expenseDate());
    }

    private static String normalizeDescription(String description) {
        return StringUtils.hasText(description) ? description.trim() : null;
    }

    private ExpenseSearchResult toSearchResult(PageResponse<ExpenseResponse> page) {
        List<ExpenseLine> items = page.content().stream().map(this::toExpenseLine).toList();
        return new ExpenseSearchResult(items, page.totalElements(), items.size());
    }

    private ExpenseResult toExpenseResult(ExpenseResponse expense) {
        return new ExpenseResult(
                expense.id(),
                expense.amount(),
                expense.category().name(),
                expense.description(),
                expense.expenseDate().toString());
    }

    private ExpenseLine toExpenseLine(ExpenseResponse expense) {
        return new ExpenseLine(
                expense.id(),
                expense.amount(),
                expense.category().name(),
                expense.description(),
                expense.expenseDate().toString());
    }
}