package com.adev.common.exchange.exception;

public class NotYetImplementedForExchangeException extends UnsupportedOperationException {
    public NotYetImplementedForExchangeException(String message) {
        super(message);
    }

    public NotYetImplementedForExchangeException() {
        this("Feature not yet implemented for exchange.");
    }
}
