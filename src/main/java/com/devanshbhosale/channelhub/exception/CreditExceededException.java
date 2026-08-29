package com.devanshbhosale.channelhub.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

/**
 * Thrown when outstanding + order total exceeds the reseller's credit limit.
 * Boundary case (outstanding + total == creditLimit) passes and never reaches this.
 */
public class CreditExceededException extends ApiException {

    private final BigDecimal creditLimit;
    private final BigDecimal outstanding;
    private final BigDecimal orderTotal;

    public CreditExceededException(BigDecimal creditLimit, BigDecimal outstanding, BigDecimal orderTotal) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "Credit limit exceeded");
        this.creditLimit = creditLimit;
        this.outstanding = outstanding;
        this.orderTotal = orderTotal;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public BigDecimal getOutstanding() {
        return outstanding;
    }

    public BigDecimal getOrderTotal() {
        return orderTotal;
    }
}
