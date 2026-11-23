package com.expenseassistant.expense.repository;

import com.expenseassistant.expense.entity.Expense;
import com.expenseassistant.expense.entity.ExpenseCategory;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public final class ExpenseSpecifications {

    private ExpenseSpecifications() {
    }

    public static Specification<Expense> forUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Expense> expenseDateFrom(LocalDate from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("expenseDate"), from);
    }

    public static Specification<Expense> expenseDateTo(LocalDate to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("expenseDate"), to);
    }

    public static Specification<Expense> categoryEquals(ExpenseCategory category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Expense> descriptionContainsIgnoreCase(String q) {
        String pattern = "%" + escapeLikeLiteral(q).toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate descriptionNotNull = cb.isNotNull(root.get("description"));
            Predicate matches = cb.like(cb.lower(root.get("description")), pattern, '\\');
            return cb.and(descriptionNotNull, matches);
        };
    }

    public static Specification<Expense> applyFilters(
            Long userId,
            LocalDate from,
            LocalDate to,
            ExpenseCategory category,
            String q) {
        Specification<Expense> spec = forUser(userId);
        if (from != null) {
            spec = spec.and(expenseDateFrom(from));
        }
        if (to != null) {
            spec = spec.and(expenseDateTo(to));
        }
        if (category != null) {
            spec = spec.and(categoryEquals(category));
        }
        if (q != null && !q.isBlank()) {
            spec = spec.and(descriptionContainsIgnoreCase(q.trim()));
        }
        return spec;
    }

    static String escapeLikeLiteral(String raw) {
        return raw.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}