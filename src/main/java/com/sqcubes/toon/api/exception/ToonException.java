package com.sqcubes.toon.api.exception;

public class ToonException extends Exception {
    public ToonException(Throwable cause) {
        super(cause);
    }

    public ToonException(String message) {
        super(message);
    }

    public ToonException(String message, Throwable cause) {
        super(message, cause);
    }
}
