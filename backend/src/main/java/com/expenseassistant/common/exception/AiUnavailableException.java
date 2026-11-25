package com.expenseassistant.common.exception;

/**
 * LLM provider failure (timeout, rate limit, bad configuration, network). Mapped to HTTP 503.
 */
public class AiUnavailableException extends RuntimeException {

    public static final String USER_MESSAGE =
            "The assistant is unavailable right now. Please try again.";

    public AiUnavailableException() {
        super(USER_MESSAGE);
    }

    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiUnavailableException(Throwable cause) {
        super(USER_MESSAGE, cause);
    }
}