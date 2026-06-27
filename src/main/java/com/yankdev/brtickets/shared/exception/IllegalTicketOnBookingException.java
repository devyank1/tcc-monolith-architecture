package com.yankdev.brtickets.shared.exception;

public class IllegalTicketOnBookingException extends RuntimeException {
    public IllegalTicketOnBookingException(String message) {
        super(message);
    }
}
