package com.adev.common.exchange.http.domain;

import com.adev.common.exchange.http.constant.HttpConfig;
import com.adev.common.exchange.http.constant.MimeTypeEnum;
import com.adev.common.exchange.http.constant.RequestMethod;

/**
 * created by admin
 * date: 2018/9/6 9:54
 */
public class RequestParam {
    /**
     * GET、POST，默认为GET
     */
    private RequestMethod method;

    /**
     * 请求的地址
     */
    private String url;
    
    /**
     * 模板url,用于做延时控制的key
     */
    private String urlTemplate;

    /**
     * 参数
     */
    private HttpDataEntity httpDataEntity;

    /**
     * 返回的数据的类型, 默认application/json
     */
    private MimeTypeEnum contentType;

    /**
     * 请求超时时间，单位是毫秒，默认5秒钟（5000）
     */
    private int requestTimeOut;
    private int socketTimeOut;
    private int connectionTimeOut;


    /**
     * 保存当前频道的key，唯一
     */
    private String channelKey;
    /**
     * 是否轮询该请求
     */
    private boolean isPolling = true;

    /**
     * 轮询间隔时间，每多少秒轮询一次，单位毫秒，默认每[5-8]秒轮询一次。
     */
    private long pollingTime;

    /**
     * 延迟发送请求。单位是毫秒，默认2秒后发送请求。
     */
    private long initialDelay;

    public String getChannelKey() {
        if(channelKey == null){
            channelKey = url;
        }
        return channelKey;
    }

    public void setChannelKey(String channelKey) {
        this.channelKey = channelKey;
    }

    public RequestMethod getMethod() {
        if(method == null){
            method = RequestMethod.GET;
        }
        return method;
    }

    public void setMethod(RequestMethod method) {
        this.method = method;
    }

    public String getUrl() {
        if(url == null){
            throw new RuntimeException("url can not null");
        }
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getUrlTemplate() {
        if(urlTemplate == null){
            throw new RuntimeException("urlTemplate can not null");
        }
        return urlTemplate;
    }

    public void setUrlTemplate(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public HttpDataEntity getHttpDataEntity() {
        if(httpDataEntity == null){
            httpDataEntity = HttpDataEntity.createHttpDataEntity();
        }
        return httpDataEntity;
    }

    public void setHttpDataEntity(HttpDataEntity httpDataEntity) {
        this.httpDataEntity = httpDataEntity;
    }

    public MimeTypeEnum getContentType() {
        if(contentType == null){
            contentType = MimeTypeEnum.APPLICATION_JSON;
        }
        return contentType;
    }

    public void setContentType(MimeTypeEnum contentType) {
        this.contentType = contentType;
    }

    public int getRequestTimeOut() {
        if(requestTimeOut == 0){
            requestTimeOut = HttpConfig.DEFAULT_REQUEST_TIMEOUT;
        }
        return requestTimeOut;
    }

    public void setRequestTimeOut(int requestTimeOut) {
        this.requestTimeOut = requestTimeOut;
    }

    public int getSocketTimeOut() {
        if(socketTimeOut == 0){
            socketTimeOut = HttpConfig.DEFAULT_SOCKET_TIMEOUT;
        }
        return socketTimeOut;
    }

    public void setSocketTimeOut(int socketTimeOut) {
        this.socketTimeOut = socketTimeOut;
    }

    public int getConnectionTimeOut() {
        if(connectionTimeOut == 0){
            connectionTimeOut = HttpConfig.DEFAULT_CONNECTION_TIMEOUT;
        }
        return connectionTimeOut;
    }

    public void setConnectionTimeOut(int connectionTimeOut) {
        this.connectionTimeOut = connectionTimeOut;
    }

    public boolean isPolling() {
        return isPolling;
    }

    public void setPolling(boolean polling) {
        isPolling = polling;
    }

    public long getPollingTime() {
        if(pollingTime == 0L){
            pollingTime = HttpConfig.DEFAULT_POLLING_TIME;
        }
        return pollingTime;
    }

    public void setPollingTime(long pollingTime) {
        this.pollingTime = pollingTime;
    }

    public long getInitialDelay() {
        if(initialDelay == 0L){
            initialDelay = HttpConfig.DEFAULT_INITIAL_DELAY;
        }
        return initialDelay;
    }

    public void setInitialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
    }
}
