package com.maepim.exception;

import com.maepim.dto.response.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles generic runtime exceptions that are not caught by more specific handlers.
     * This is useful for catching service-layer exceptions (e.g., "Username is already taken!").
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<MessageResponse> handleGenericRuntimeException(RuntimeException ex, WebRequest request) {
        logger.error("An unexpected error occurred: {}", ex.getMessage());
        MessageResponse message = new MessageResponse(ex.getMessage());
        return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
    }
}