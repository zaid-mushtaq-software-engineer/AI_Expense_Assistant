package com.expenseassistant.expense.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.expenseassistant.common.exception.ResourceNotFoundException;
import com.expenseassistant.common.exception.ValidationFailedException;
import com.expenseassistant.expense.dto.CreateExpenseRequest;
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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        expenseService = new ExpenseService(expenseRepository, userRepository, expenseMapper, CLOCK);
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