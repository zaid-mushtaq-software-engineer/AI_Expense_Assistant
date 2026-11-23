package com.expenseassistant.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TotalSpendingResponse(LocalDate from, LocalDate to, BigDecimal totalAmount, long expenseCount) {}