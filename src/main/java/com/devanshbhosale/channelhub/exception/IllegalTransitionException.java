package com.devanshbhosale.channelhub.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown for invalid order state machine moves (including cancelling a non-PENDING order).
 */
public class IllegalTransitionException extends ApiException {

    public IllegalTransitionException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
