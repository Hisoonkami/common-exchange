package com.adev.common.exchange.webscoket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionCache {
    public enum ConnectionState {
        IDLE,
        DELAY_CONNECT,
        CONNECTED,
        CONNECTING,
        CLOSED_ON_ERROR
    }

    /**
     * 连接状态
     */
    public static Map<String, ConnectionState> collectStatusMap = new ConcurrentHashMap<>();
}
