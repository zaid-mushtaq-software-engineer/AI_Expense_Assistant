package com.expenseassistant.assistant.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expenseassistant.assistant.dto.PendingActionResponse;
import com.expenseassistant.assistant.service.ChatTurnContext;
import com.expenseassistant.assistant.service.CreatedPendingAction;
import com.expenseassistant.assistant.service.DeleteExpensePayload;
import com.expenseassistant.assistant.service.PendingActionFormatter;
import com.expenseassistant.assistant.service.PendingActionService;
import com.expenseassistant.assistant.service.PendingActionType;
import com.expenseassistant.assistant.service.UpdateExpensePayload;
import com.expenseassistant.assistant.tools.ToolResults.ExpenseSearchResult;
import com.expenseassistant.assistant.tools.ToolResults.PendingResult;
import com.expenseassistant.assistant.tools.ToolResults.ToolOutcome;
import com.expenseassistant.common.exception.ResourceNotFoundException;
import com.expenseassistant.expense.dto.UpdateExpenseRequest;
import com.expenseassistant.expense.service.ExpenseWriteValidator;
import jakarta.validation.Validation;
import java.time.Clock;
import com.expenseassistant.common.exception.ValidationFailedException;
import com.expenseassistant.common.response.ErrorResponse.FieldError;
import com.expenseassistant.common.response.PageResponse;
import com.expenseassistant.expense.dto.CreateExpenseRequest;
import com.expenseassistant.expense.dto.ExpenseFilter;
import com.expenseassistant.expense.dto.ExpenseResponse;
import com.expenseassistant.expense.entity.ExpenseCategory;
import com.expenseassistant.expense.service.ExpenseService;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.ToolParam;

@ExtendWith(MockitoExtension.class)
class ExpenseToolsTest {

    private static final ToolContext USER1 = new ToolContext(Map.of(AssistantToolContextKeys.USER_ID, 1L));

    @Mock
    private ExpenseService expenseService;

    @Mock
    private PendingActionService pendingActionService;

    private final ChatTurnContext chatTurnContext = new ChatTurnContext();
    private final AssistantToolSupport toolSupport =
            AssistantToolSupportTestHelper.toolSupport(chatTurnContext);
    private final PendingActionFormatter pendingActionFormatter = new PendingActionFormatter();
    private final ExpenseWriteValidator expenseWriteValidator =
            new ExpenseWriteValidator(
                    Validation.buildDefaultValidatorFactory().getValidator(),
                    Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC));

    private ExpenseTools expenseTools;

    @BeforeEach
    void setUp() {
        expenseTools =
                new ExpenseTools(
                        expenseService,
                        pendingActionService,
                        pendingActionFormatter,
                        expenseWriteValidator,
                        chatTurnContext,
                        toolSupport);
    }

    @Test
    void searchExpenses_mapsPageAndCapsAtReturnedCount() {
        List<ExpenseResponse> rows =
                List.of(expenseRow(1L, "100"), expenseRow(2L, "200"), expenseRow(3L, "50"));
        PageResponse<ExpenseResponse> page = new PageResponse<>(rows, 0, 20, 57, 3);
        when(expenseService.search(eq(1L), any(ExpenseFilter.class), eq(0), eq(20), isNull()))
                .thenReturn(page);

        ToolOutcome<ExpenseSearchResult> outcome =
                expenseTools.searchExpenses("2026-11-01", "2026-11-25", null, null, USER1);

        assertThat(outcome.error()).isNull();
        assertThat(outcome.success().items()).hasSize(3);
        assertThat(outcome.success().totalElements()).isEqualTo(57);
        assertThat(outcome.success().returnedCount()).isEqualTo(3);

        ArgumentCaptor<ExpenseFilter> filterCaptor = ArgumentCaptor.forClass(ExpenseFilter.class);
        verify(expenseService).search(eq(1L), filterCaptor.capture(), eq(0), eq(20), isNull());
        assertThat(filterCaptor.getValue().from()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(filterCaptor.getValue().to()).isEqualTo(LocalDate.of(2026, 11, 25));
    }

    @Test
    void searchExpenses_emptyList() {
        when(expenseService.search(eq(1L), any(), eq(0), eq(20), isNull()))
                .thenReturn(new PageResponse<>(Collections.emptyList(), 0, 20, 0, 0));

        ToolOutcome<ExpenseSearchResult> outcome = expenseTools.searchExpenses(null, null, null, null, USER1);

        assertThat(outcome.success().items()).isEmpty();
        assertThat(outcome.success().totalElements()).isZero();
    }

    @Test
    void searchExpenses_invalidCategory_returnsError() {
        ToolOutcome<ExpenseSearchResult> outcome =
                expenseTools.searchExpenses(null, null, "NOT_REAL", null, USER1);

        assertThat(outcome.success()).isNull();
        assertThat(outcome.error().code()).isEqualTo("INVALID_CATEGORY");
    }

    @Test
    void searchExpenses_invalidDate_returnsError() {
        ToolOutcome<ExpenseSearchResult> outcome =
                expenseTools.searchExpenses("bad-date", null, null, null, USER1);

        assertThat(outcome.error().code()).isEqualTo("INVALID_DATE");
    }

    @Test
    void addExpense_success() {
        LocalDate date = LocalDate.of(2026, 11, 24);
        ExpenseResponse saved =
                new ExpenseResponse(10L, new BigDecimal("500"), ExpenseCategory.FOOD, "Dinner", date, Instant.now(), Instant.now());
        when(expenseService.create(eq(1L), any(CreateExpenseRequest.class))).thenReturn(saved);

        ToolOutcome<ToolResults.ExpenseResult> outcome =
                expenseTools.addExpense(new BigDecimal("500"), "FOOD", "Dinner", "2026-11-24", USER1);

        assertThat(outcome.success().id()).isEqualTo(10L);
        assertThat(outcome.success().category()).isEqualTo("FOOD");
    }

    @Test
    void addExpense_validationFailure_returnsErrorWithFields() {
        when(expenseService.create(eq(1L), any()))
                .thenThrow(new ValidationFailedException(
                        "Request validation failed",
                        List.of(new FieldError("amount", "must be greater than 0"))));

        ToolOutcome<ToolResults.ExpenseResult> outcome =
                expenseTools.addExpense(BigDecimal.ZERO, "FOOD", null, "2026-11-24", USER1);

        assertThat(outcome.error().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(outcome.error().message()).contains("amount");
        verify(expenseService).create(eq(1L), any());
    }

    @Test
    void addExpense_invalidCategory_neverCallsCreate() {
        ToolOutcome<ToolResults.ExpenseResult> outcome =
                expenseTools.addExpense(new BigDecimal("10"), "BAD", null, "2026-11-24", USER1);

        assertThat(outcome.error().code()).isEqualTo("INVALID_CATEGORY");
        verify(expenseService, never()).create(any(), any());
    }

    @Test
    void searchExpenses_exposedParamsExcludeUserId() throws NoSuchMethodException {
        assertToolParamCount(
                "searchExpenses", 4, String.class, String.class, String.class, String.class, ToolContext.class);
    }

    @Test
    void addExpense_exposedParamsExcludeUserId() throws NoSuchMethodException {
        assertToolParamCount(
                "addExpense",
                4,
                BigDecimal.class,
                String.class,
                String.class,
                String.class,
                ToolContext.class);
    }

    @Test
    void proposeExpenseDeletion_createsPendingAction_doesNotDelete() {
        ExpenseResponse expense = expenseRow(9L, "500");
        UUID actionId = UUID.randomUUID();
        Instant expires = Instant.parse("2026-11-25T12:10:00Z");
        String summary = pendingActionFormatter.formatDelete(expense);
        when(expenseService.get(1L, 9L)).thenReturn(expense);
        when(pendingActionService.create(
                        eq(1L),
                        isNull(),
                        eq(PendingActionType.DELETE_EXPENSE),
                        eq(new DeleteExpensePayload(9L)),
                        eq(summary)))
                .thenReturn(new CreatedPendingAction(actionId, expires, PendingActionType.DELETE_EXPENSE, summary));

        ToolOutcome<PendingResult> outcome = expenseTools.proposeExpenseDeletion(9L, USER1);

        assertThat(outcome.error()).isNull();
        assertThat(outcome.success().actionId()).isEqualTo(actionId.toString());
        assertThat(outcome.success().type()).isEqualTo("DELETE_EXPENSE");
        assertThat(chatTurnContext.getPendingAction())
                .contains(new PendingActionResponse(actionId, PendingActionType.DELETE_EXPENSE, summary, expires));
        verify(expenseService, never()).delete(any(), any());
    }

    @Test
    void proposeExpenseDeletion_notFound_returnsError() {
        when(expenseService.get(1L, 99L)).thenThrow(new ResourceNotFoundException("Expense not found"));

        ToolOutcome<PendingResult> outcome = expenseTools.proposeExpenseDeletion(99L, USER1);

        assertThat(outcome.success()).isNull();
        assertThat(outcome.error().code()).isEqualTo("NOT_FOUND");
        verify(pendingActionService, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void proposeExpenseUpdate_mergeAmountOnly() {
        ExpenseResponse current = expenseRow(3L, "500");
        UUID actionId = UUID.randomUUID();
        Instant expires = Instant.parse("2026-11-25T12:10:00Z");
        UpdateExpenseRequest merged =
                new UpdateExpenseRequest(
                        new BigDecimal("650"),
                        ExpenseCategory.FOOD,
                        current.description(),
                        current.expenseDate());
        String summary = pendingActionFormatter.formatUpdate(current, merged);
        when(expenseService.get(1L, 3L)).thenReturn(current);
        when(pendingActionService.create(
                        eq(1L),
                        isNull(),
                        eq(PendingActionType.UPDATE_EXPENSE),
                        eq(new UpdateExpensePayload(3L, merged)),
                        eq(summary)))
                .thenReturn(new CreatedPendingAction(actionId, expires, PendingActionType.UPDATE_EXPENSE, summary));

        ToolOutcome<PendingResult> outcome =
                expenseTools.proposeExpenseUpdate(3L, new BigDecimal("650"), null, null, null, USER1);

        assertThat(outcome.success().actionId()).isEqualTo(actionId.toString());
        verify(expenseService, never()).update(any(), any(), any());
    }

    @Test
    void proposeExpenseUpdate_noChanges_returnsValidationError() {
        ExpenseResponse current = expenseRow(4L, "100");
        when(expenseService.get(1L, 4L)).thenReturn(current);

        ToolOutcome<PendingResult> outcome =
                expenseTools.proposeExpenseUpdate(4L, new BigDecimal("100"), null, null, null, USER1);

        assertThat(outcome.error().code()).isEqualTo("VALIDATION_ERROR");
        assertThat(outcome.error().message()).contains("No changes");
        verify(pendingActionService, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void proposeExpenseUpdate_invalidAmount_returnsValidationError() {
        ExpenseResponse current = expenseRow(5L, "100");
        when(expenseService.get(1L, 5L)).thenReturn(current);

        ToolOutcome<PendingResult> outcome =
                expenseTools.proposeExpenseUpdate(5L, BigDecimal.ZERO, null, null, null, USER1);

        assertThat(outcome.error().code()).isEqualTo("VALIDATION_ERROR");
        verify(pendingActionService, never()).create(any(), any(), any(), any(), any());
    }

    private ExpenseResponse expenseRow(long id, String amount) {
        LocalDate date = LocalDate.of(2026, 11, 5);
        return new ExpenseResponse(
                id, new BigDecimal(amount), ExpenseCategory.FOOD, "item", date, Instant.now(), Instant.now());
    }

    private void assertToolParamCount(String methodName, int expectedToolParams, Class<?>... paramTypes)
            throws NoSuchMethodException {
        Method method = ExpenseTools.class.getMethod(methodName, paramTypes);
        long toolParamCount =
                Arrays.stream(method.getParameters()).filter(p -> p.isAnnotationPresent(ToolParam.class)).count();
        assertThat(toolParamCount).isEqualTo(expectedToolParams);
        assertThat(Arrays.stream(method.getParameters()).map(Parameter::getType).anyMatch(ToolContext.class::equals))
                .isTrue();
    }
}