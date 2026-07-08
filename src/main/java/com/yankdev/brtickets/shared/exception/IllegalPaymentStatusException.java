package com.yankdev.brtickets.shared.exception;

public class IllegalPaymentStatusException extends RuntimeException {
    public IllegalPaymentStatusException(String message) {
        super(message);
    }
}
