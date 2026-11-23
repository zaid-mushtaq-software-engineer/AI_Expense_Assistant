package com.expenseassistant.expense.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expenseassistant.common.exception.GlobalExceptionHandler;
import com.expenseassistant.common.exception.ValidationFailedException;
import com.expenseassistant.expense.dto.CreateExpenseRequest;
import com.expenseassistant.expense.dto.ExpenseResponse;
import com.expenseassistant.expense.entity.ExpenseCategory;
import com.expenseassistant.expense.service.ExpenseService;
import com.expenseassistant.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExpenseController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc
class ExpenseControllerTest {

    private static final String EXPENSES = "/api/v1/expenses";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExpenseService expenseService;

    @MockitoBean
    private CurrentUserProvider currentUserProvider;

    @Test
    void create_returns201WithLocationAndBody() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        Instant now = Instant.parse("2026-11-23T08:15:30Z");
        ExpenseResponse response = new ExpenseResponse(
                5012L,
                new BigDecimal("500.00"),
                ExpenseCategory.FOOD,
                "Dinner",
                LocalDate.of(2026, 11, 22),
                now,
                now);
        when(expenseService.create(eq(1L), any(CreateExpenseRequest.class))).thenReturn(response);

        mockMvc.perform(post(EXPENSES)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "amount": 500.00,
                                  "category": "FOOD",
                                  "description": "Dinner",
                                  "expenseDate": "2026-11-22"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", EXPENSES + "/5012"))
                .andExpect(jsonPath("$.id").value(5012))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.category").value("FOOD"))
                .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    void create_rejectsZeroAmount() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);

        mockMvc.perform(post(EXPENSES)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "amount": 0,
                                  "category": "FOOD",
                                  "expenseDate": "2026-11-22"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='amount')]").exists());
    }

    @Test
    void create_rejectsNegativeAmount() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);

        mockMvc.perform(post(EXPENSES)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "amount": -10.00,
                                  "category": "FOOD",
                                  "expenseDate": "2026-11-22"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void create_rejectsTooManyDecimalPlaces() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);

        mockMvc.perform(post(EXPENSES)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "amount": 10.123,
                                  "category": "FOOD",
                                  "expenseDate": "2026-11-22"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='amount')]").exists());
    }

    @Test
    void create_rejectsMissingCategory() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);

        mockMvc.perform(post(EXPENSES)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "amount": 10.00,
                                  "expenseDate": "2026-11-22"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='category')]").exists());
    }

    @Test
    void create_mapsServiceValidationFailure() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        when(expenseService.create(eq(1L), any(CreateExpenseRequest.class)))
                .thenThrow(new ValidationFailedException("Expense date cannot be more than one day in the future"));

        mockMvc.perform(post(EXPENSES)
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "amount": 10.00,
                                  "category": "FOOD",
                                  "expenseDate": "2030-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Expense date cannot be more than one day in the future"));
    }
}