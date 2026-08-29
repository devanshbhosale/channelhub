package com.devanshbhosale.channelhub.exception;

import com.devanshbhosale.channelhub.dto.ErrorRes;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every error leaves the API as JSON. Simple errors are {"error": "..."}; stock and credit
 * failures carry their contract-defined extra fields.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage());
        if (ex instanceof InsufficientStockException stock) {
            body.put("details", stock.getDetails());
        }
        if (ex instanceof CreditExceededException credit) {
            body.put("creditLimit", credit.getCreditLimit());
            body.put("outstanding", credit.getOutstanding());
            body.put("orderTotal", credit.getOrderTotal());
        }
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRes> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(fieldError -> fieldError.getDefaultMessage())
            .orElse("Invalid request");
        return ResponseEntity.badRequest().body(new ErrorRes(message));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorRes> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorRes("Not found"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRes> handleUnexpected(Exception ex) {
        return ResponseEntity.status(500).body(new ErrorRes("Internal server error"));
    }
}
