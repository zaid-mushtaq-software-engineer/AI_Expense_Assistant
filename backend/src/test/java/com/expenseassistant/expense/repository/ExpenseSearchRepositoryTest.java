package com.expenseassistant.expense.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.expenseassistant.config.JpaConfig;
import com.expenseassistant.expense.entity.Expense;
import com.expenseassistant.expense.entity.ExpenseCategory;
import com.expenseassistant.expense.service.ExpenseSearchSupport;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Import(JpaConfig.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ActiveProfiles("dev")
class ExpenseSearchRepositoryTest {

    private static final String PLACEHOLDER_BCRYPT =
            "$2a$10$abcdefghijklmnopqrstuuABCDEFGHIJKLMNOPQRSTU";

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

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
    void search_paginatesOwnedRowsOnly() {
        LocalDate base = LocalDate.of(2026, 6, 1);
        for (int i = 0; i < 25; i++) {
            expenseRepository.save(new Expense(
                    userA,
                    new BigDecimal(i + 1),
                    ExpenseCategory.FOOD,
                    "Item " + i,
                    base.plusDays(i)));
        }
        expenseRepository.save(new Expense(
                userB,
                new BigDecimal("99.00"),
                ExpenseCategory.OTHER,
                "User B only",
                base));

        var page0 = expenseRepository.findAll(
                ExpenseSpecifications.applyFilters(userA.getId(), null, null, null, null),
                ExpenseSearchSupport.toPageable(0, 10, null));
        var page2 = expenseRepository.findAll(
                ExpenseSpecifications.applyFilters(userA.getId(), null, null, null, null),
                ExpenseSearchSupport.toPageable(2, 10, null));

        assertThat(page0.getTotalElements()).isEqualTo(25);
        assertThat(page0.getTotalPages()).isEqualTo(3);
        assertThat(page0.getContent()).hasSize(10);
        assertThat(page2.getContent()).hasSize(5);

        var userBPage = expenseRepository.findAll(
                ExpenseSpecifications.applyFilters(userB.getId(), null, null, null, null),
                ExpenseSearchSupport.toPageable(0, 50, null));
        assertThat(userBPage.getTotalElements()).isEqualTo(1);
        assertThat(userBPage.getContent().getFirst().getDescription()).isEqualTo("User B only");
    }

    @Test
    void search_sortsByAmountAscendingWithStableIdTiebreaker() {
        LocalDate day = LocalDate.of(2026, 3, 1);
        expenseRepository.save(
                new Expense(userA, new BigDecimal("50.00"), ExpenseCategory.FOOD, "a", day));
        expenseRepository.save(
                new Expense(userA, new BigDecimal("10.00"), ExpenseCategory.FOOD, "b", day));
        expenseRepository.save(
                new Expense(userA, new BigDecimal("30.00"), ExpenseCategory.FOOD, "c", day));

        var page = expenseRepository.findAll(
                ExpenseSpecifications.applyFilters(userA.getId(), null, null, null, null),
                ExpenseSearchSupport.toPageable(0, 20, "amount,asc"));

        assertThat(page.getContent())
                .extracting(Expense::getAmount)
                .containsExactly(
                        new BigDecimal("10.00"), new BigDecimal("30.00"), new BigDecimal("50.00"));
    }

    @Test
    void search_filtersByDateCategoryAndDescription() {
        expenseRepository.save(new Expense(
                userA,
                new BigDecimal("10.00"),
                ExpenseCategory.FOOD,
                "Lunch at cafe",
                LocalDate.of(2026, 1, 10)));
        expenseRepository.save(new Expense(
                userA,
                new BigDecimal("20.00"),
                ExpenseCategory.TRANSPORTATION,
                "Bus pass",
                LocalDate.of(2026, 1, 15)));
        expenseRepository.save(new Expense(
                userA,
                new BigDecimal("30.00"),
                ExpenseCategory.FOOD,
                "Dinner",
                LocalDate.of(2026, 1, 20)));

        Specification<Expense> spec = ExpenseSpecifications.applyFilters(
                userA.getId(),
                LocalDate.of(2026, 1, 10),
                LocalDate.of(2026, 1, 15),
                ExpenseCategory.FOOD,
                "cafe");

        var page = expenseRepository.findAll(spec, ExpenseSearchSupport.toPageable(0, 20, null));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getDescription()).isEqualTo("Lunch at cafe");
    }

    @Test
    void search_descriptionFilterIsCaseInsensitiveAndEscapesWildcards() {
        expenseRepository.save(new Expense(
                userA,
                new BigDecimal("5.00"),
                ExpenseCategory.FOOD,
                "100% OFF",
                LocalDate.of(2026, 2, 1)));
        expenseRepository.save(new Expense(
                userA,
                new BigDecimal("6.00"),
                ExpenseCategory.FOOD,
                "regular",
                LocalDate.of(2026, 2, 2)));

        var match = expenseRepository.findAll(
                ExpenseSpecifications.applyFilters(userA.getId(), null, null, null, "%"),
                ExpenseSearchSupport.toPageable(0, 20, null));
        var caseInsensitive = expenseRepository.findAll(
                ExpenseSpecifications.applyFilters(userA.getId(), null, null, null, "off"),
                ExpenseSearchSupport.toPageable(0, 20, null));

        assertThat(match.getTotalElements()).isEqualTo(1);
        assertThat(caseInsensitive.getTotalElements()).isEqualTo(1);
    }
}