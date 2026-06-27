package com.yankdev.brtickets.shared.exception;

public class IllegalBookingCancellingException extends RuntimeException {
    public IllegalBookingCancellingException(String message) {
        super(message);
    }
}
