package com.expenseassistant.analytics.repository;

import com.expenseassistant.expense.entity.Expense;
import com.expenseassistant.expense.entity.ExpenseCategory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ExpenseAnalyticsRepository {

    private final EntityManager entityManager;

    public ExpenseAnalyticsRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public TotalAggregateRow sumTotal(Long userId, LocalDate from, LocalDate to) {
        TypedQuery<Object[]> query = entityManager.createQuery(
                """
                SELECT COALESCE(SUM(e.amount), 0), COUNT(e)
                FROM Expense e
                WHERE e.user.id = :userId
                  AND e.expenseDate BETWEEN :from AND :to
                """,
                Object[].class);
        query.setParameter("userId", userId);
        query.setParameter("from", from);
        query.setParameter("to", to);
        Object[] row = query.getSingleResult();
        BigDecimal total = (BigDecimal) row[0];
        Long count = (Long) row[1];
        return new TotalAggregateRow(total, count != null ? count : 0L);
    }

    public List<CategoryAggregateRow> sumByCategory(
            Long userId, LocalDate from, LocalDate to, ExpenseCategory categoryFilter) {
        StringBuilder jpql =
                new StringBuilder(
                        """
                        SELECT e.category, SUM(e.amount), COUNT(e)
                        FROM Expense e
                        WHERE e.user.id = :userId
                          AND e.expenseDate BETWEEN :from AND :to
                        """);
        if (categoryFilter != null) {
            jpql.append(" AND e.category = :category");
        }
        jpql.append(
                """
                 GROUP BY e.category
                 ORDER BY SUM(e.amount) DESC, e.category ASC
                """);

        TypedQuery<Object[]> query = entityManager.createQuery(jpql.toString(), Object[].class);
        query.setParameter("userId", userId);
        query.setParameter("from", from);
        query.setParameter("to", to);
        if (categoryFilter != null) {
            query.setParameter("category", categoryFilter);
        }
        return query.getResultList().stream()
                .map(row -> new CategoryAggregateRow(
                        (ExpenseCategory) row[0], (BigDecimal) row[1], (Long) row[2]))
                .toList();
    }

    public List<Expense> findTopExpensesByAmount(Long userId, LocalDate from, LocalDate to, int limit) {
        TypedQuery<Expense> query = entityManager.createQuery(
                """
                SELECT e FROM Expense e
                WHERE e.user.id = :userId
                  AND e.expenseDate BETWEEN :from AND :to
                ORDER BY e.amount DESC, e.id DESC
                """,
                Expense.class);
        query.setParameter("userId", userId);
        query.setParameter("from", from);
        query.setParameter("to", to);
        query.setMaxResults(limit);
        return query.getResultList();
    }

    public record TotalAggregateRow(BigDecimal totalAmount, long expenseCount) {}

    public record CategoryAggregateRow(ExpenseCategory category, BigDecimal amount, long expenseCount) {}
}