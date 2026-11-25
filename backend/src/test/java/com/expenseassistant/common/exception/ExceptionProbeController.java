package com.expenseassistant.common.exception;

import com.expenseassistant.expense.entity.ExpenseCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/_test/exceptions")
class ExceptionProbeController {

    @GetMapping("/not-found")
    void notFound() {
        throw new ResourceNotFoundException("Expense not found");
    }

    @GetMapping("/validation-failed")
    void validationFailed() {
        throw new ValidationFailedException("Business rule violated");
    }

    @GetMapping("/boom")
    void boom() {
        throw new RuntimeException("secret internal detail");
    }

    @GetMapping("/ai-unavailable")
    void aiUnavailable() {
        throw new AiUnavailableException();
    }

    @GetMapping("/action-expired")
    void actionExpired() {
        throw new PendingActionExpiredException();
    }

    @PostMapping("/valid-body")
    void validBody(@Valid @RequestBody ProbeRequest request) {
    }

    @PostMapping("/json-body")
    void jsonBody(@RequestBody Map<String, Object> body) {
    }

    @GetMapping("/bad-enum")
    void badEnum(@RequestParam ExpenseCategory category) {
    }

    record ProbeRequest(
            @NotNull String name, @NotNull @DecimalMin(value = "0.01", message = "must be greater than 0") BigDecimal amount) {
    }
}