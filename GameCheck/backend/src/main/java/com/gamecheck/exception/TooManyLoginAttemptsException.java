package com.gamecheck.exception;

public class TooManyLoginAttemptsException extends RuntimeException {

    public TooManyLoginAttemptsException(String message) {
        super(message);
    }
}
