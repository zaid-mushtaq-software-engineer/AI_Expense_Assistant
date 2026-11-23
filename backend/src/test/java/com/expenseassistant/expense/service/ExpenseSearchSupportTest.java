package com.expenseassistant.expense.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.expenseassistant.common.exception.ValidationFailedException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class ExpenseSearchSupportTest {

    @Test
    void toPageable_defaultsSortAndClampsSize() {
        Pageable pageable = ExpenseSearchSupport.toPageable(0, 1000, null);

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(100);
        assertThat(pageable.getSort().getOrderFor("expenseDate").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(pageable.getSort().getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void toPageable_acceptsAllowedSortField() {
        Pageable pageable = ExpenseSearchSupport.toPageable(1, 10, "amount,asc");

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("amount").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void toPageable_rejectsUnknownSortField() {
        assertThatThrownBy(() -> ExpenseSearchSupport.toPageable(0, 20, "unknown,desc"))
                .isInstanceOf(ValidationFailedException.class)
                .hasMessageContaining("Invalid sort field");
    }
}