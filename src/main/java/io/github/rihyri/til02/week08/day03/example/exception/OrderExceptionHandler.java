package io.github.rihyri.til02.week08.day03.example.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(basePackages = "io.github.rihyri.til02.week08.day03.example.controller")
public class OrderExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(
                Map.of(
                        "result", false,
                        "message", exception.getMessage()
                )
        );
    }
}
