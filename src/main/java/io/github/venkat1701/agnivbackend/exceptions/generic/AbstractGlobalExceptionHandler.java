package io.github.venkat1701.agnivbackend.exceptions.generic;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public abstract class AbstractGlobalExceptionHandler extends RuntimeException {

    public abstract ResponseEntity<ErrorResponse> handleException(Exception ex);

    protected ResponseEntity<ErrorResponse> buildErrorResponse(Exception ex, HttpStatus status, String message) {
        ErrorResponse errorResponse = ErrorResponse.builder(ex, status, message).build();
        return new ResponseEntity<>(errorResponse, status);
    }
}