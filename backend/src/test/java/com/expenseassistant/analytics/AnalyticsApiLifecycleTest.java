package com.expenseassistant.analytics;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.expenseassistant.security.DevCurrentUserProvider;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AnalyticsApiLifecycleTest {

    private static final String EXPENSES = "/api/v1/expenses";
    private static final String ANALYTICS = "/api/v1/analytics";
    private static final String USER_HEADER = DevCurrentUserProvider.DEV_USER_HEADER;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanIsolatedRange() {
        jdbcTemplate.update(
                "DELETE FROM expenses WHERE user_id IN (1, 2) AND expense_date BETWEEN '2026-10-01' AND '2026-10-31'");
    }

    @Test
    void analyticsIsolationAndAggregates() throws Exception {
        LocalDate d1 = LocalDate.of(2026, 10, 5);
        LocalDate d2 = LocalDate.of(2026, 10, 15);
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);

        createExpense("1", "400.00", "FOOD", "lunch", d1);
        createExpense("1", "600.00", "BILLS", "power", d2);
        createExpense("2", "9999.00", "FOOD", "other user", d1);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(ANALYTICS + "/total")
                        .header(USER_HEADER, "1")
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(1000.00))
                .andExpect(jsonPath("$.expenseCount").value(2));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        ANALYTICS + "/by-category")
                        .header(USER_HEADER, "1")
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].category").value("BILLS"))
                .andExpect(jsonPath("$.categories[0].percentage").value(60.00))
                .andExpect(jsonPath("$.categories[1].category").value("FOOD"))
                .andExpect(jsonPath("$.categories[1].percentage").value(40.00));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                        ANALYTICS + "/highest-category")
                        .header(USER_HEADER, "1")
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("BILLS"))
                .andExpect(jsonPath("$.amount").value(600.00));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(ANALYTICS + "/report")
                        .header(USER_HEADER, "1")
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averagePerDay").value(closeTo(32.26, 0.01)))
                .andExpect(jsonPath("$.topExpenses.length()").value(2))
                .andExpect(jsonPath("$.highestCategory").value("BILLS"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(ANALYTICS + "/total")
                        .header(USER_HEADER, "2")
                        .queryParam("from", from.toString())
                        .queryParam("to", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(9999.00))
                .andExpect(jsonPath("$.expenseCount").value(1));
    }

    private void createExpense(String userId, String amount, String category, String description, LocalDate date)
            throws Exception {
        String body =
                """
                {
                  "amount": %s,
                  "category": "%s",
                  "description": "%s",
                  "expenseDate": "%s"
                }
                """
                        .formatted(amount, category, description, date);
        mockMvc.perform(post(EXPENSES).header(USER_HEADER, userId).contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
    }
}