package com.example.bankcards.exception;

import com.example.bankcards.exception.constant.ErrorStatus;
import lombok.Getter;

@Getter
public class ApiErrorException extends RuntimeException {

    private final ErrorStatus errorStatus;

    public ApiErrorException(ErrorStatus errorStatus) {
        super(errorStatus.getMessage());
        this.errorStatus = errorStatus;
    }
}
