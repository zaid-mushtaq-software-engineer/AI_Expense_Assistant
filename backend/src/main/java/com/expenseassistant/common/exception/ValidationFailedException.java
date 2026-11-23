package com.expenseassistant.common.exception;

import com.expenseassistant.common.response.ErrorResponse.FieldError;
import java.util.Collections;
import java.util.List;

public class ValidationFailedException extends RuntimeException {

    private final List<FieldError> fieldErrors;

    public ValidationFailedException(String message) {
        this(message, Collections.emptyList());
    }

    public ValidationFailedException(String message, List<FieldError> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors != null ? List.copyOf(fieldErrors) : Collections.emptyList();
    }

    public List<FieldError> getFieldErrors() {
        return fieldErrors;
    }
}