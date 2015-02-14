package com.sqcubes.toon.api.exception;

import com.sqcubes.toon.api.model.ToonLoginResponse;

public class ToonLoginFailedException extends ToonException {
    public ToonLoginFailedException(Throwable cause) {
        super(cause);
    }

    public ToonLoginFailedException(String message) {
        super(message);
    }

    public ToonLoginFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ToonLoginFailedException(String message, ToonLoginResponse response) {
        super(message + " Response: " + response);
    }
}
