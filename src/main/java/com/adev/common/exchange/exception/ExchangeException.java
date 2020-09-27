package com.adev.common.exchange.exception;

/**
 * @author: xianninig
 * @date: 2018/8/29 16:33
 */
public class ExchangeException extends RuntimeException {
    public ExchangeException(String message) {
        super(message);
    }

    public ExchangeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExchangeException(Throwable cause) {
        super(cause);
    }
}
