package com.devanshbhosale.channelhub.exception;

import com.devanshbhosale.channelhub.dto.StockShortfall;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * Thrown when one or more products cannot be fully allocated across warehouses.
 * Carries one shortfall record per short product (all shortfalls are collected before throwing).
 */
public class InsufficientStockException extends ApiException {

    private final List<StockShortfall> details;

    public InsufficientStockException(List<StockShortfall> details) {
        super(HttpStatus.CONFLICT, "Insufficient stock for requested quantities");
        this.details = List.copyOf(details);
    }

    public List<StockShortfall> getDetails() {
        return details;
    }
}
