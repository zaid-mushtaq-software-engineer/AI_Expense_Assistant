package com.expenseassistant.expense.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentCaptor;

import com.expenseassistant.common.response.PageResponse;
import com.expenseassistant.expense.dto.ExpenseFilter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.expenseassistant.common.exception.ResourceNotFoundException;
import com.expenseassistant.common.exception.ValidationFailedException;
import com.expenseassistant.expense.dto.CreateExpenseRequest;
import com.expenseassistant.expense.dto.UpdateExpenseRequest;
import com.expenseassistant.expense.dto.ExpenseResponse;
import com.expenseassistant.expense.entity.Expense;
import com.expenseassistant.expense.entity.ExpenseCategory;
import com.expenseassistant.expense.mapper.ExpenseMapper;
import com.expenseassistant.expense.repository.ExpenseRepository;
import com.expenseassistant.user.entity.User;
import com.expenseassistant.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-23T06:30:00Z"), ZONE);

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private UserRepository userRepository;

    private final ExpenseMapper expenseMapper = new ExpenseMapper();

    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        ExpenseWriteValidator writeValidator = new ExpenseWriteValidator(validator, CLOCK);
        expenseService = new ExpenseService(expenseRepository, userRepository, expenseMapper, writeValidator);
    }

    @Test
    void create_savesAndReturnsResponse() {
        User user = new User("Dev", "dev@example.com", "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        LocalDate expenseDate = LocalDate.now(CLOCK);
        CreateExpenseRequest request =
                new CreateExpenseRequest(new BigDecimal("500.00"), ExpenseCategory.FOOD, "Dinner", expenseDate);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
            Expense expense = invocation.getArgument(0);
            expenseMapper.toResponse(expense);
            return expense;
        });

        ExpenseResponse response = expenseService.create(1L, request);

        verify(expenseRepository).save(any(Expense.class));
        assertThat(response.amount()).isEqualByComparingTo("500.00");
        assertThat(response.category()).isEqualTo(ExpenseCategory.FOOD);
        assertThat(response.description()).isEqualTo("Dinner");
        assertThat(response.expenseDate()).isEqualTo(expenseDate);
    }

    @Test
    void create_allowsExpenseDateOneDayAhead() {
        User user = new User("Dev", "dev@example.com", "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        LocalDate tomorrow = LocalDate.now(CLOCK).plusDays(1);
        CreateExpenseRequest request =
                new CreateExpenseRequest(new BigDecimal("10.00"), ExpenseCategory.BILLS, null, tomorrow);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        expenseService.create(1L, request);

        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    void create_rejectsExpenseDateMoreThanOneDayAhead() {
        LocalDate tooFar = LocalDate.now(CLOCK).plusDays(2);
        CreateExpenseRequest request =
                new CreateExpenseRequest(new BigDecimal("10.00"), ExpenseCategory.FOOD, null, tooFar);

        assertThatThrownBy(() -> expenseService.create(1L, request))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("one day in the future");

        verify(expenseRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void search_rejectsFromAfterTo() {
        ExpenseFilter filter =
                new ExpenseFilter(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1), null, null);

        assertThatThrownBy(() -> expenseService.search(1L, filter, 0, 20, null))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessage("from must be on or before to");

        verify(expenseRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void search_rejectsInvalidSortField() {
        ExpenseFilter filter = new ExpenseFilter(null, null, null, null);

        assertThatThrownBy(() -> expenseService.search(1L, filter, 0, 20, "unknown,desc"))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("Invalid sort field");
    }

    @Test
    void search_returnsMappedPage() {
        User user = new User("Dev", "dev@example.com", "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        Expense expense = new Expense(
                user, new BigDecimal("10.00"), ExpenseCategory.FOOD, "Snack", LocalDate.now(CLOCK));
        when(expenseRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(expense), Pageable.ofSize(20), 1));

        PageResponse<ExpenseResponse> response =
                expenseService.search(1L, new ExpenseFilter(null, null, null, null), 0, 20, null);

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(20);
    }

    @Test
    void get_returnsMappedResponse() {
        User user = new User("Dev", "dev@example.com", "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        LocalDate expenseDate = LocalDate.of(2026, 11, 22);
        Expense expense = new Expense(
                user, new BigDecimal("500.00"), ExpenseCategory.FOOD, "Dinner", expenseDate);
        Instant now = Instant.parse("2026-11-23T08:15:30Z");
        ReflectionTestUtils.setField(expense, "id", 5012L);
        ReflectionTestUtils.setField(expense, "createdAt", now);
        ReflectionTestUtils.setField(expense, "updatedAt", now);

        when(expenseRepository.findByIdAndUserId(5012L, 1L)).thenReturn(Optional.of(expense));

        ExpenseResponse response = expenseService.get(1L, 5012L);

        assertThat(response.id()).isEqualTo(5012L);
        assertThat(response.amount()).isEqualByComparingTo("500.00");
        assertThat(response.category()).isEqualTo(ExpenseCategory.FOOD);
        assertThat(response.description()).isEqualTo("Dinner");
        assertThat(response.expenseDate()).isEqualTo(expenseDate);
        verify(expenseRepository).findByIdAndUserId(5012L, 1L);
    }

    @Test
    void get_unknownId_notFound() {
        when(expenseRepository.findByIdAndUserId(999L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.get(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Expense not found");
    }

    @Test
    void get_otherUsersExpense_notFound() {
        when(expenseRepository.findByIdAndUserId(5012L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.get(2L, 5012L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Expense not found");
    }

    @Test
    void update_appliesAllFields() {
        User user = new User("Dev", "dev@example.com", "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        Expense expense = new Expense(
                user, new BigDecimal("10.00"), ExpenseCategory.FOOD, "Old", LocalDate.now(CLOCK));
        ReflectionTestUtils.setField(expense, "id", 7L);
        UpdateExpenseRequest request = new UpdateExpenseRequest(
                new BigDecimal("25.50"),
                ExpenseCategory.BILLS,
                "Updated",
                LocalDate.now(CLOCK).minusDays(1));

        when(expenseRepository.findByIdAndUserId(7L, 1L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(expense)).thenAnswer(invocation -> invocation.getArgument(0));

        ExpenseResponse response = expenseService.update(1L, 7L, request);

        assertThat(response.amount()).isEqualByComparingTo("25.50");
        assertThat(response.category()).isEqualTo(ExpenseCategory.BILLS);
        assertThat(response.description()).isEqualTo("Updated");
        verify(expenseRepository).save(expense);
    }

    @Test
    void update_notFound() {
        when(expenseRepository.findByIdAndUserId(404L, 1L)).thenReturn(Optional.empty());
        UpdateExpenseRequest request =
                new UpdateExpenseRequest(new BigDecimal("1.00"), ExpenseCategory.FOOD, null, LocalDate.now(CLOCK));

        assertThatThrownBy(() -> expenseService.update(1L, 404L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Expense not found");

        verify(expenseRepository, never()).save(any());
    }

    @Test
    void update_rejectsFutureDateBeyondOneDay() {
        LocalDate tooFar = LocalDate.now(CLOCK).plusDays(2);
        UpdateExpenseRequest request =
                new UpdateExpenseRequest(new BigDecimal("1.00"), ExpenseCategory.FOOD, null, tooFar);

        assertThatThrownBy(() -> expenseService.update(1L, 1L, request))
                .isInstanceOf(ValidationFailedException.class);

        verify(expenseRepository, never()).findByIdAndUserId(any(), any());
    }

    @Test
    void delete_removesOwnedExpense() {
        User user = new User("Dev", "dev@example.com", "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        Expense expense = new Expense(
                user, new BigDecimal("5.00"), ExpenseCategory.FOOD, null, LocalDate.now(CLOCK));
        when(expenseRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(expense));

        expenseService.delete(1L, 3L);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).delete(captor.capture());
        assertThat(captor.getValue()).isSameAs(expense);
    }

    @Test
    void delete_notFound() {
        when(expenseRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.delete(1L, 3L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Expense not found");

        verify(expenseRepository, never()).delete(org.mockito.ArgumentMatchers.<Expense>any());
    }

    @Test
    void create_unknownUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        CreateExpenseRequest request = new CreateExpenseRequest(
                new BigDecimal("1.00"),
                ExpenseCategory.OTHER,
                null,
                LocalDate.now(CLOCK));

        assertThatThrownBy(() -> expenseService.create(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}