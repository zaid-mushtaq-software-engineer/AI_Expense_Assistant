package com.expenseassistant.common.exception;

public class PendingActionExpiredException extends RuntimeException {

    public static final String USER_MESSAGE = "This confirmation has expired. Please ask again.";

    public PendingActionExpiredException() {
        super(USER_MESSAGE);
    }

    public PendingActionExpiredException(String message) {
        super(message);
    }
}