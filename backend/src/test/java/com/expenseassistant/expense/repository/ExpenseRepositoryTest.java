package com.expenseassistant.expense.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.expenseassistant.config.JpaConfig;
import com.expenseassistant.expense.entity.Expense;
import com.expenseassistant.expense.entity.ExpenseCategory;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("dev")
class ExpenseRepositoryTest {

    private static final String PLACEHOLDER_BCRYPT =
            "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTU";

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User userA;
    private User userB;

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
    }

    @Test
    void saveAndFetch() {
        Expense saved = expenseRepository.save(new Expense(
                userA,
                new BigDecimal("125.50"),
                ExpenseCategory.FOOD,
                "Lunch",
                LocalDate.of(2026, 1, 15)));

        assertThat(saved.getId()).isNotNull();
        assertThat(expenseRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void zeroAmountRejectedByDatabase() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO expenses (user_id, amount, category, description, expense_date)
                VALUES (?, 0.00, 'FOOD', 'bad', '2026-01-01')
                """,
                userA.getId()))
                .hasMessageContaining("chk_expenses_amount_positive");
    }

    @Test
    void invalidCategoryRejectedByDatabase() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                INSERT INTO expenses (user_id, amount, category, description, expense_date)
                VALUES (?, 10.00, 'INVALID', 'bad', '2026-01-01')
                """,
                userA.getId()))
                .hasMessageContaining("chk_expenses_category");
    }

    @Test
    void findByIdAndUserId_enforcesOwnership() {
        Expense saved = expenseRepository.save(new Expense(
                userA,
                new BigDecimal("50.00"),
                ExpenseCategory.TRANSPORTATION,
                "Bus",
                LocalDate.of(2026, 1, 10)));

        assertThat(expenseRepository.findByIdAndUserId(saved.getId(), userA.getId())).isPresent();
        assertThat(expenseRepository.findByIdAndUserId(saved.getId(), userB.getId())).isEmpty();
    }
}