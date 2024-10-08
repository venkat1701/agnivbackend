package io.github.venkat1701.agnivbackend.exceptions.ollama;

import io.github.venkat1701.agnivbackend.exceptions.constants.ExceptionStatusCodes;
import io.github.venkat1701.agnivbackend.exceptions.generic.AbstractGlobalExceptionHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpServerErrorException;


public class ChatException extends AbstractGlobalExceptionHandler {

    private String message;
    public ChatException(String message) {
        this.message = message;
    }

    @ExceptionHandler({RuntimeException.class, HttpServerErrorException.GatewayTimeout.class})
    @Override
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        return buildErrorResponse(ex, HttpStatus.valueOf(ExceptionStatusCodes.INTERNAL_SERVER_ERROR.getCode()), message);
    }
}
