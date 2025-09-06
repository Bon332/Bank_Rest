package com.example.bankcards.exception.constant;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorStatus {
    USER_NOT_FOUND("User not found", HttpStatus.NOT_FOUND),
    CARD_NOT_FOUND("Card not found", HttpStatus.NOT_FOUND),
    INSUFFICIENT_FUNDS("Insufficient funds", HttpStatus.CONFLICT),
    FORBIDDEN_OPERATION("Operation not allowed", HttpStatus.FORBIDDEN),
    VALIDATION_ERROR("Validation failed", HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String message;
    private final HttpStatus httpStatus;

    ErrorStatus(String message, HttpStatus httpStatus) {
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
