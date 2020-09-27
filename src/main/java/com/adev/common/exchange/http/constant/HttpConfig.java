package com.adev.common.exchange.http.constant;

/**
 * @author xn
 * 2018/11/28 16:57
 */
public class HttpConfig {

    /**
     * 设置整个连接池的最大连接数.
     * 当前有28个交易所使用http，每个交易所有3个路由，每个路由最大两个连接数，大概计算得=28x3x2=168，再计算1.5倍：168x1.5=252，所以连接数最大设置252个足以。
     */
    public static final Integer POOL_MAX_TOTAL = 4000;

    /**
     * 设置每个路由的最大连接数
     * 比如百度最多只能用2个连接数，单独设置：connManager.setMaxPerRoute(new HttpRoute(new HttpHost("http://baidu.com", 80)), 2);
     */
    public static final Integer POOL_MAX_PER_ROUTE = 300;

    /**
     * validateAfterInactivity 空闲永久连接检查间隔，这个牵扯的还比较多.<br />
     * 官方推荐使用这个来检查永久链接的可用性，而不推荐每次请求的时候才去检查.
     */
    public static final Integer POOL_VALIDATE_AFTER_INACTIVITY = 2000;




    /**
     * 请求超时时间，httpclient使用连接池来管理连接，这个时间就是从连接池获取连接的超时时间，单位毫秒
     *
     */
    public static final Integer DEFAULT_REQUEST_TIMEOUT = 15000;

    /**
     * 套接字超时时间，数据传输过程中数据包之间间隔的最大时间，单位毫秒
     */
    public static final Integer DEFAULT_SOCKET_TIMEOUT = 20000;

    /**
     * 默认连接超时时间，连接建立时间，三次握手完成时间，单位毫秒
     */
    public static final Integer DEFAULT_CONNECTION_TIMEOUT = 10000;

    /**
     * 轮询时间，单位毫秒
     */
    public static final Integer DEFAULT_POLLING_TIME = 10000;

    /**
     * 延迟发送请求，单位毫秒
     */
    public static final Integer DEFAULT_INITIAL_DELAY = 10000;
}
