package com.adev.common.exchange.http.manage;

import com.adev.common.exchange.http.constant.HttpConfig;
import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * @author: xianninig
 * @date: 2018/8/29 16:33
 */
public class LocalHttpConnectionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalHttpConnectionManager.class);

    private static PoolingHttpClientConnectionManager manager = null;

    static {
        LayeredConnectionSocketFactory socketFactory = null;

        try {
            socketFactory = new SSLConnectionSocketFactory(SSLContext.getDefault());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory> create()
                .register("https", socketFactory).register("http", new PlainConnectionSocketFactory()).build();
        manager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        //设置整个连接池的最大连接数
        manager.setMaxTotal(HttpConfig.POOL_MAX_TOTAL);
        //设置每个路由的最大连接数。
        //比如百度最多只能用2个连接数，单独设置：connManager.setMaxPerRoute(new HttpRoute(new HttpHost("http://baidu.com", 80)), 2);
        manager.setDefaultMaxPerRoute(HttpConfig.POOL_MAX_PER_ROUTE);
        //validateAfterInactivity 空闲永久连接检查间隔，这个牵扯的还比较多
        //官方推荐使用这个来检查永久链接的可用性，而不推荐每次请求的时候才去检查
        manager.setValidateAfterInactivity(HttpConfig.POOL_VALIDATE_AFTER_INACTIVITY);


        new IdleConnectionMonitorTask(manager).lanuchTimer();
    }

    private static HttpRequestRetryHandler getHttpRequestRetryHandler() {

        HttpRequestRetryHandler httpRequestRetryHandler = new HttpRequestRetryHandler() {

            @Override
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                /**
                 * 设置重试的次数，这里设置不重试
                 */
                if (executionCount >= 1) {
                    return false;
                }
                /**
                 * 如果服务器丢掉了连接，那么就重试
                 */
                if (exception instanceof NoHttpResponseException) {
                    LOGGER.debug("NoHttpResponseException, retry...");
                    return true;
                }
                /**
                 * 不要重试SSL握手异常
                 */
                if (exception instanceof SSLHandshakeException) {
                    return false;
                }
                /**
                 * 超时
                 */
                if (exception instanceof InterruptedIOException) {
                    return false;
                }
                /**
                 * 目标服务器不可达
                 */
                if (exception instanceof UnknownHostException) {
                    return false;
                }
                /**
                 * 连接被拒绝
                 */
                if (exception instanceof ConnectTimeoutException) {
                    return false;
                }
                /**
                 * SSL握手异常
                 */
                if (exception instanceof SSLException) {
                    return false;
                }

                HttpClientContext clientContext = HttpClientContext.adapt(context);
                HttpRequest request = clientContext.getRequest();
                // 如果请求是幂等的，就再次尝试
                if (!(request instanceof HttpEntityEnclosingRequest)) {
                    LOGGER.debug("HttpEntityEnclosingRequest, retry...");
                    return true;
                }
                return false;
            }
        };

        return httpRequestRetryHandler;
    }

    private static ConnectionKeepAliveStrategy getConnectionKeepAliveStrategy(){
        //DefaultConnectionKeepAliveStrategy 默认实现
        ConnectionKeepAliveStrategy myStrategy = new ConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
                Args.notNull(response, "HTTP response");
                final HeaderElementIterator it = new BasicHeaderElementIterator(
                        response.headerIterator(HTTP.CONN_KEEP_ALIVE));
                while (it.hasNext()) {
                    final HeaderElement he = it.nextElement();
                    final String param = he.getName();
                    final String value = he.getValue();
                    if (value != null && param.equalsIgnoreCase("timeout")) {
                        try {
                            return Long.parseLong(value) * 1000;
                        } catch (final NumberFormatException ignore) {
                        }
                    }
                }
                return 1;
            }

        };

        return myStrategy;
    }

    public static CloseableHttpClient getHttpClient() {
        try {
            HostnameVerifier hv = new HostnameVerifier() {
                @Override
                public boolean verify(String urlHostName, SSLSession session) {
                    LOGGER.warn("Warning: URL Host: " + urlHostName + " vs. " + session.getPeerHost());
                    return true;
                }
            };

            //创建SSLContext
            SSLContext sslContext = SSLContext.getInstance("SSL");
            TrustManager[] tm = new TrustManager[] { new MyX509TrustManager() };
            //初始化
            sslContext.init(null, tm, null);
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);

            //获取httpClient
            HttpClientBuilder httpClientBuilder =
                    HttpClients.custom().setSSLHostnameVerifier(hv).setSSLSocketFactory(sslsf)
                            .setConnectionManager(manager).setRetryHandler(getHttpRequestRetryHandler());


            /*if (false) {
                HttpHost proxy = new HttpHost("127.0.0.1", 1090, "https");
                httpClientBuilder.setProxy(proxy);
            }*/


            RequestConfig requestConfig = RequestConfig.custom()
                            .setConnectTimeout(HttpConfig.DEFAULT_CONNECTION_TIMEOUT)
                            .setConnectionRequestTimeout(HttpConfig.DEFAULT_REQUEST_TIMEOUT)
                            .setSocketTimeout(HttpConfig.DEFAULT_SOCKET_TIMEOUT)
                            .build();

            httpClientBuilder.setDefaultRequestConfig(requestConfig);

            //ConnectionConfig connectionConfig = ConnectionConfig.custom().setBufferSize(4128).build();
            //httpClientBuilder.setDefaultConnectionConfig(connectionConfig);

            //httpClientBuilder.setKeepAliveStrategy(getConnectionKeepAliveStrategy());

            CloseableHttpClient httpClient = httpClientBuilder.build();
            /*CloseableHttpClient httpClient = HttpClients.createDefault();//如果不采用连接池就是这种方式获取连接*/
            return httpClient;
        } catch (Exception e) {

        }

        return null;
    }

    /**
     * 监控有异常的链接
     */
    private static class IdleConnectionMonitorTask {
        private final HttpClientConnectionManager connectionManager;

        public ScheduledExecutorService scheduExec = Executors.newScheduledThreadPool(1);

        public IdleConnectionMonitorTask(HttpClientConnectionManager connMgr) {
            super();
            this.connectionManager = connMgr;
        }

        /**
         * 定期清理空闲链接的任务
         */
        public void lanuchTimer() {
            final Runnable task = () -> {
                // 关闭失效的连接
                connectionManager.closeExpiredConnections();
                // 可选的, 关闭10秒内不活动的连接
                connectionManager.closeIdleConnections(10, TimeUnit.SECONDS);
            };
            scheduExec.scheduleWithFixedDelay(task, 1000 * 5, 1000 * 3, TimeUnit.MILLISECONDS);
        }

    }

}
