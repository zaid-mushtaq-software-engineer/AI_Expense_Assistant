package com.expenseassistant.expense.service;

import com.expenseassistant.common.exception.ValidationFailedException;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class ExpenseSearchSupport {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    private static final String DEFAULT_SORT = "expenseDate,desc";
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of("expenseDate", "amount", "createdAt");

    private ExpenseSearchSupport() {
    }

    public static Pageable toPageable(int page, int size, String sortParam) {
        if (page < 0) {
            throw new ValidationFailedException("page must be 0 or greater");
        }
        int effectiveSize = size;
        if (effectiveSize <= 0) {
            effectiveSize = DEFAULT_SIZE;
        }
        effectiveSize = Math.min(effectiveSize, MAX_SIZE);
        Sort sort = parseSort(sortParam == null || sortParam.isBlank() ? DEFAULT_SORT : sortParam.trim());
        return PageRequest.of(page, effectiveSize, sort);
    }

    static Sort parseSort(String sortParam) {
        String[] parts = sortParam.split(",", 2);
        if (parts.length == 0 || parts[0].isBlank()) {
            throw new ValidationFailedException("Invalid sort parameter");
        }
        String property = parts[0].trim();
        if (!ALLOWED_SORT_PROPERTIES.contains(property)) {
            throw new ValidationFailedException("Invalid sort field: " + property);
        }
        Sort.Direction direction = Sort.Direction.DESC;
        if (parts.length == 2 && !parts[1].isBlank()) {
            try {
                direction = Sort.Direction.fromString(parts[1].trim());
            } catch (IllegalArgumentException ex) {
                throw new ValidationFailedException("Invalid sort direction: " + parts[1].trim());
            }
        }
        return Sort.by(new Sort.Order(direction, property)).and(Sort.by(Sort.Order.desc("id")));
    }
}