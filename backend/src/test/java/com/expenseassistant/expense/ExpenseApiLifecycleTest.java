package com.expenseassistant.expense;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expenseassistant.security.DevCurrentUserProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ExpenseApiLifecycleTest {

    private static final String EXPENSES = "/api/v1/expenses";
    private static final String USER_HEADER = DevCurrentUserProvider.DEV_USER_HEADER;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullCrudLifecycle_withTwoUserIsolation() throws Exception {
        LocalDate expenseDate = LocalDate.now().minusDays(1);
        String createJson =
                """
                {
                  "amount": 42.00,
                  "category": "FOOD",
                  "description": "lifecycle lunch",
                  "expenseDate": "%s"
                }
                """
                        .formatted(expenseDate);

        MvcResult createResult = mockMvc.perform(post(EXPENSES)
                        .header(USER_HEADER, "1")
                        .contentType(APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long id = created.get("id").asLong();
        long updatedAtBeforeMs = Instant.parse(created.get("updatedAt").asText()).toEpochMilli();

        mockMvc.perform(get(EXPENSES)
                        .header(USER_HEADER, "1")
                        .queryParam("category", "FOOD")
                        .queryParam("q", "lifecycle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id==" + id + ")]").exists());

        MvcResult updateResult = mockMvc.perform(put(EXPENSES + "/" + id)
                        .header(USER_HEADER, "1")
                        .contentType(APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "amount": 55.50,
                                  "category": "BILLS",
                                  "description": "lifecycle updated",
                                  "expenseDate": "%s"
                                }
                                """
                                        .formatted(expenseDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(55.50))
                .andExpect(jsonPath("$.category").value("BILLS"))
                .andReturn();

        JsonNode updated = objectMapper.readTree(updateResult.getResponse().getContentAsString());
        long updatedAtAfterMs = Instant.parse(updated.get("updatedAt").asText()).toEpochMilli();

        assertThat(Instant.parse(updated.get("createdAt").asText()).toEpochMilli())
                .isEqualTo(Instant.parse(created.get("createdAt").asText()).toEpochMilli());
        assertThat(updatedAtAfterMs).isGreaterThanOrEqualTo(updatedAtBeforeMs);

        mockMvc.perform(get(EXPENSES + "/" + id).header(USER_HEADER, "2"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put(EXPENSES + "/" + id)
                        .header(USER_HEADER, "2")
                        .contentType(APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete(EXPENSES + "/" + id).header(USER_HEADER, "2"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get(EXPENSES + "/" + id).header(USER_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("lifecycle updated"));

        mockMvc.perform(delete(EXPENSES + "/" + id).header(USER_HEADER, "1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(EXPENSES + "/" + id).header(USER_HEADER, "1"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete(EXPENSES + "/" + id).header(USER_HEADER, "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listReturnsOnlyCallerRows() throws Exception {
        mockMvc.perform(get(EXPENSES).header(USER_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(0)));

        mockMvc.perform(get(EXPENSES).header(USER_HEADER, "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(0)));
    }
}