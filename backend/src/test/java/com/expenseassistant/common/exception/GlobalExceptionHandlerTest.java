package com.expenseassistant.common.exception;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ExceptionProbeController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    private static final String BASE = "/api/v1/_test/exceptions";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void notFound_returnsStandardShape() throws Exception {
        mockMvc.perform(get(BASE + "/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Expense not found"))
                .andExpect(jsonPath("$.path").value(BASE + "/not-found"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void validationFailed_returns400() throws Exception {
        mockMvc.perform(get(BASE + "/validation-failed"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Business rule violated"));
    }

    @Test
    void unexpectedError_returnsGeneric500WithoutLeak() throws Exception {
        mockMvc.perform(get(BASE + "/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.fieldErrors").value(nullValue()));
    }

    @Test
    void beanValidation_returnsFieldErrors() throws Exception {
        mockMvc.perform(post(BASE + "/valid-body")
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\": null, \"amount\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(2)));
    }

    @Test
    void malformedJson_returns400() throws Exception {
        mockMvc.perform(post(BASE + "/json-body")
                        .contentType(APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Malformed JSON body"));
    }

    @Test
    void badEnumQueryParam_returns400() throws Exception {
        mockMvc.perform(get(BASE + "/bad-enum").queryParam("category", "NOT_A_CATEGORY"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'category'"));
    }
}