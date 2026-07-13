package com.minibank.mini_core_banking.global;

import com.minibank.mini_core_banking.domain.account.exception.ErrorCode;
import com.minibank.mini_core_banking.domain.account.exception.CustomException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<?> handleCustomException(CustomException e) {
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(errorResponse(e.getErrorCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return ResponseEntity
                .badRequest()
                .body(errorResponse(ErrorCode.INVALID_REQUEST, "Invalid request"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        return ResponseEntity
                .internalServerError()
                .body(errorResponse(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage()));
    }

    private ErrorResponse errorResponse(ErrorCode errorCode, String message) {
        return new ErrorResponse(
                RequestIdHolder.getOrCreate(),
                errorCode.name(),
                message,
                LocalDateTime.now()
        );
    }
}
