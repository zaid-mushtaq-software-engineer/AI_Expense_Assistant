package com.expenseassistant.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CategorySpendingResponse(
        LocalDate from, LocalDate to, BigDecimal totalAmount, List<CategoryTotal> categories) {}