package com.expenseassistant.expense.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expenseassistant.common.exception.GlobalExceptionHandler;
import com.expenseassistant.common.exception.ResourceNotFoundException;
import com.expenseassistant.common.exception.ValidationFailedException;
import com.expenseassistant.common.response.PageResponse;
import com.expenseassistant.expense.dto.CreateExpenseRequest;
import com.expenseassistant.expense.dto.ExpenseFilter;
import com.expenseassistant.expense.dto.ExpenseResponse;
import com.expenseassistant.expense.dto.UpdateExpenseRequest;
import com.expenseassistant.expense.entity.ExpenseCategory;
import com.expenseassistant.expense.service.ExpenseService;
import com.expenseassistant.security.CurrentUserProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
    void list_returns200PageResponse() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        Instant now = Instant.parse("2026-11-23T08:15:30Z");
        ExpenseResponse row = new ExpenseResponse(
                1L,
                new BigDecimal("10.00"),
                ExpenseCategory.FOOD,
                "Snack",
                LocalDate.of(2026, 11, 20),
                now,
                now);
        when(expenseService.search(eq(1L), any(ExpenseFilter.class), eq(0), eq(20), eq(null)))
                .thenReturn(new PageResponse<>(List.of(row), 0, 20, 1, 1));

        mockMvc.perform(get(EXPENSES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void list_passesFilterQueryParams() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        when(expenseService.search(
                        eq(1L),
                        any(ExpenseFilter.class),
                        eq(0),
                        eq(20),
                        org.mockito.ArgumentMatchers.isNull()))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get(EXPENSES)
                        .queryParam("from", "2026-01-01")
                        .queryParam("to", "2026-01-31")
                        .queryParam("category", "FOOD")
                        .queryParam("q", "lunch"))
                .andExpect(status().isOk());
    }

    @Test
    void list_rejectsInvalidCategory() throws Exception {
        mockMvc.perform(get(EXPENSES).queryParam("category", "NOT_A_CATEGORY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'category'"));
    }

    @Test
    void list_rejectsInvalidDate() throws Exception {
        mockMvc.perform(get(EXPENSES).queryParam("from", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'from'"));
    }

    @Test
    void list_mapsFromAfterToValidation() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        when(expenseService.search(eq(1L), any(ExpenseFilter.class), eq(0), eq(20), eq(null)))
                .thenThrow(new ValidationFailedException("from must be on or before to"));

        mockMvc.perform(get(EXPENSES).queryParam("from", "2026-02-01").queryParam("to", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("from must be on or before to"));
    }

    @Test
    void get_returns200WithBody() throws Exception {
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
        when(expenseService.get(1L, 5012L)).thenReturn(response);

        mockMvc.perform(get(EXPENSES + "/5012"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5012))
                .andExpect(jsonPath("$.amount").value(500.00))
                .andExpect(jsonPath("$.category").value("FOOD"))
                .andExpect(jsonPath("$.description").value("Dinner"))
                .andExpect(jsonPath("$.expenseDate").value("2026-11-22"))
                .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    void get_notFound_returns404() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        when(expenseService.get(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Expense not found"));

        mockMvc.perform(get(EXPENSES + "/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Expense not found"));
    }

    @Test
    void get_nonNumericId_returns400() throws Exception {
        mockMvc.perform(get(EXPENSES + "/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    @Test
    void update_returns200WithBody() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        Instant now = Instant.parse("2026-11-23T08:15:30Z");
        ExpenseResponse updated = new ExpenseResponse(
                5012L,
                new BigDecimal("75.00"),
                ExpenseCategory.BILLS,
                "Updated",
                LocalDate.of(2026, 11, 21),
                now,
                now);
        when(expenseService.update(eq(1L), eq(5012L), any(UpdateExpenseRequest.class))).thenReturn(updated);

        mockMvc.perform(put(EXPENSES + "/5012")
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "amount": 75.00,
                                  "category": "BILLS",
                                  "description": "Updated",
                                  "expenseDate": "2026-11-21"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(75.00))
                .andExpect(jsonPath("$.category").value("BILLS"))
                .andExpect(jsonPath("$.userId").doesNotExist());
    }

    @Test
    void update_rejectsInvalidBody() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);

        mockMvc.perform(put(EXPENSES + "/1")
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
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void delete_returns204() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);

        mockMvc.perform(delete(EXPENSES + "/5012")).andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        when(currentUserProvider.requireUserId()).thenReturn(1L);
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Expense not found"))
                .when(expenseService)
                .delete(1L, 999L);

        mockMvc.perform(delete(EXPENSES + "/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Expense not found"));
    }

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