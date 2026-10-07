package com.aura.coffeeshop.common.exception;

import org.springframework.http.HttpStatus;

/** A request that is well-formed but not allowed in the current state (bad promo code, invalid status transition, ...). */
public class BusinessRuleException extends RuntimeException {

    private final HttpStatus status;

    public BusinessRuleException(String message) {
        this(HttpStatus.CONFLICT, message);
    }

    public BusinessRuleException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static BusinessRuleException badRequest(String message) {
        return new BusinessRuleException(HttpStatus.BAD_REQUEST, message);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
