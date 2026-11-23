package com.expenseassistant.analytics.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.expenseassistant.config.JpaConfig;
import com.expenseassistant.expense.entity.Expense;
import com.expenseassistant.expense.entity.ExpenseCategory;
import com.expenseassistant.expense.repository.ExpenseRepository;
import com.expenseassistant.user.entity.User;
import com.expenseassistant.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import({JpaConfig.class, ExpenseAnalyticsRepository.class})
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("dev")
class ExpenseAnalyticsRepositoryTest {

    private static final String PLACEHOLDER_BCRYPT =
            "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTU";

    @Autowired
    private ExpenseAnalyticsRepository expenseAnalyticsRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    private User userA;
    private User userB;
    private LocalDate nov1;
    private LocalDate nov30;

    @BeforeEach
    void setUp() {
        userA = userRepository.save(new User(
                "User A",
                "a-" + UUID.randomUUID() + "@example.com",
                PLACEHOLDER_BCRYPT));
        userB = userRepository.save(new User(
                "User B",
                "b-" + UUID.randomUUID() + "@example.com",
                PLACEHOLDER_BCRYPT));
        nov1 = LocalDate.of(2026, 11, 1);
        nov30 = LocalDate.of(2026, 11, 30);
    }

    @Test
    void sumTotal_includesBoundaries_andExcludesOtherUsers() {
        expenseRepository.save(new Expense(
                userA, new BigDecimal("100.00"), ExpenseCategory.FOOD, "a", nov1));
        expenseRepository.save(new Expense(
                userA, new BigDecimal("50.50"), ExpenseCategory.BILLS, "b", nov30));
        expenseRepository.save(new Expense(
                userA, new BigDecimal("999.00"), ExpenseCategory.FOOD, "out", nov30.plusDays(1)));
        expenseRepository.save(new Expense(
                userB, new BigDecimal("1000.00"), ExpenseCategory.FOOD, "other", nov1));

        var total = expenseAnalyticsRepository.sumTotal(userA.getId(), nov1, nov30);

        assertThat(total.totalAmount()).isEqualByComparingTo("150.50");
        assertThat(total.expenseCount()).isEqualTo(2);
    }

    @Test
    void sumByCategory_sortsByAmountThenCategoryOnTie() {
        expenseRepository.save(new Expense(
                userA, new BigDecimal("50.00"), ExpenseCategory.BILLS, "bills", LocalDate.of(2026, 11, 10)));
        expenseRepository.save(new Expense(
                userA, new BigDecimal("50.00"), ExpenseCategory.FOOD, "food", LocalDate.of(2026, 11, 11)));
        expenseRepository.save(new Expense(
                userA, new BigDecimal("100.00"), ExpenseCategory.TRAVEL, "travel", LocalDate.of(2026, 11, 12)));

        var rows = expenseAnalyticsRepository.sumByCategory(userA.getId(), nov1, nov30, null);

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).category()).isEqualTo(ExpenseCategory.TRAVEL);
        assertThat(rows.get(1).category()).isEqualTo(ExpenseCategory.BILLS);
        assertThat(rows.get(2).category()).isEqualTo(ExpenseCategory.FOOD);
    }

    @Test
    void findTopExpensesByAmount_returnsFiveLargest() {
        for (int i = 1; i <= 7; i++) {
            expenseRepository.save(new Expense(
                    userA,
                    new BigDecimal(i * 10 + ".00"),
                    ExpenseCategory.OTHER,
                    "e" + i,
                    LocalDate.of(2026, 11, i)));
        }

        var top = expenseAnalyticsRepository.findTopExpensesByAmount(userA.getId(), nov1, nov30, 5);

        assertThat(top).hasSize(5);
        assertThat(top.getFirst().getAmount()).isEqualByComparingTo("70.00");
        assertThat(top.get(4).getAmount()).isEqualByComparingTo("30.00");
    }
}