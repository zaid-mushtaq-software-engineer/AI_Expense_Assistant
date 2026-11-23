package com.expenseassistant.expense.service;

import com.expenseassistant.common.exception.ResourceNotFoundException;
import com.expenseassistant.common.exception.ValidationFailedException;
import com.expenseassistant.common.response.PageResponse;
import com.expenseassistant.expense.dto.CreateExpenseRequest;
import com.expenseassistant.expense.dto.ExpenseFilter;
import com.expenseassistant.expense.dto.ExpenseResponse;
import com.expenseassistant.expense.repository.ExpenseSpecifications;
import com.expenseassistant.expense.entity.Expense;
import com.expenseassistant.expense.mapper.ExpenseMapper;
import com.expenseassistant.expense.repository.ExpenseRepository;
import com.expenseassistant.user.entity.User;
import com.expenseassistant.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final ExpenseMapper expenseMapper;
    private final Clock clock;

    public ExpenseService(
            ExpenseRepository expenseRepository,
            UserRepository userRepository,
            ExpenseMapper expenseMapper,
            Clock clock) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.expenseMapper = expenseMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> search(Long userId, ExpenseFilter filter, int page, int size, String sort) {
        validateDateRange(filter.from(), filter.to());
        Pageable pageable = ExpenseSearchSupport.toPageable(page, size, sort);
        Specification<Expense> spec = ExpenseSpecifications.applyFilters(
                userId, filter.from(), filter.to(), filter.category(), filter.q());
        return PageResponse.from(expenseRepository.findAll(spec, pageable).map(expenseMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(Long userId, Long id) {
        return expenseRepository
                .findByIdAndUserId(id, userId)
                .map(expenseMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
    }

    @Transactional
    public ExpenseResponse create(Long userId, CreateExpenseRequest request) {
        validateExpenseDate(request.expenseDate());
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Expense expense = new Expense(
                user,
                request.amount(),
                request.category(),
                request.description(),
                request.expenseDate());
        return expenseMapper.toResponse(expenseRepository.save(expense));
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationFailedException("from must be on or before to");
        }
    }

    private void validateExpenseDate(LocalDate expenseDate) {
        LocalDate today = LocalDate.now(clock);
        LocalDate maxAllowed = today.plusDays(1);
        if (expenseDate.isAfter(maxAllowed)) {
            throw new ValidationFailedException("Expense date cannot be more than one day in the future");
        }
    }
}