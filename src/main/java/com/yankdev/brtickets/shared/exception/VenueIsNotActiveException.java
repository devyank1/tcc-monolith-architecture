package com.yankdev.brtickets.shared.exception;

public class VenueIsNotActiveException extends RuntimeException {
    public VenueIsNotActiveException(String message) {
        super(message);
    }
}
