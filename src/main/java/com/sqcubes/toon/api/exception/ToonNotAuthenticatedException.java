package com.sqcubes.toon.api.exception;

public class ToonNotAuthenticatedException extends ToonLoginFailedException {
    public ToonNotAuthenticatedException(String message) {
        super(message);
    }
}
