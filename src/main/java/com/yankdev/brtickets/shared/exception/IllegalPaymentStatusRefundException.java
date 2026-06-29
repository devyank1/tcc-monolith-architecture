package com.yankdev.brtickets.shared.exception;

public class IllegalPaymentStatusRefundException extends RuntimeException {
    public IllegalPaymentStatusRefundException(String message) {
        super(message);
    }
}
