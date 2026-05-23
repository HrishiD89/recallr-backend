package com.recallr;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationErrors(MethodArgumentNotValidException ex) {
        // Mapping validation failure to 411 as per your requirement
        return ResponseEntity.status(411).body("Error in inputs");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleInternalErrors(Exception ex) {
        return ResponseEntity.status(500).body("Internal server error");
    }
}