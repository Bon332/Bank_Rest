package com.example.bankcards.exception.handler;

import com.example.bankcards.exception.ApiErrorException;
import com.example.bankcards.exception.constant.ErrorStatus;
import com.example.bankcards.exception.dto.ErrorResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ExceptionHandler {

    @org.springframework.web.bind.annotation.ExceptionHandler(ApiErrorException.class)
    public ResponseEntity<ErrorResponseDto> handleApiException(ApiErrorException ex) {
        ErrorStatus errorStatus = ex.getErrorStatus();

        log.warn("Handled business exception: {} ({})",
                errorStatus.getMessage(), errorStatus.getHttpStatus());

        return ResponseEntity
                .status(errorStatus.getHttpStatus())
                .body(new ErrorResponseDto(errorStatus.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ErrorResponseDto> handleResponseStatusException(org.springframework.web.server.ResponseStatusException ex) {
        log.warn("Handled ResponseStatusException: {} ({})", ex.getReason(), ex.getStatusCode());

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(new ErrorResponseDto(ex.getReason()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleAllExceptions(Exception ex) {
        log.error("Unexpected error occurred", ex);

        ErrorStatus errorStatus = ErrorStatus.INTERNAL_ERROR;
        return ResponseEntity
                .status(errorStatus.getHttpStatus())
                .body(new ErrorResponseDto(errorStatus.getMessage()
                        + ": " + ex.getClass().getSimpleName()));
    }

}
