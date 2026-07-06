package com.yankdev.brtickets.shared.exception;

public class UserIsNotActiveException extends RuntimeException {
    public UserIsNotActiveException(String message) {
        super(message);
    }
}
