package com.adev.common.exchange.exception;

/**
 * @author: xianninig
 * @date: 2018/8/29 16:33
 */
public class HttpClientUtilException extends RuntimeException {
    public HttpClientUtilException(String message) {
        super(message);
    }

    public HttpClientUtilException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpClientUtilException(Throwable cause) {
        super(cause);
    }
}
