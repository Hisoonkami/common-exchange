package com.adev.common.exchange.http.client;

import com.adev.common.exchange.exception.HttpClientUtilException;
import com.adev.common.exchange.http.constant.HttpConfig;
import com.adev.common.exchange.http.constant.MimeTypeEnum;
import com.adev.common.exchange.http.constant.RequestMethod;
import com.adev.common.exchange.http.domain.HttpDataEntity;
import com.adev.common.exchange.http.domain.RequestParam;
import com.adev.common.exchange.http.manage.LocalHttpConnectionManager;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author: xianninig
 * @date: 2018/8/29 16:35
 */
public class HttpClientUtil {
    private static final Logger LOG = LoggerFactory.getLogger(HttpClientUtil.class);

    public static String sendHttp(RequestParam requestParam) {
        String response = null;
        if (requestParam.getMethod() == RequestMethod.GET) {
            response = doGet(requestParam);
        } else if (requestParam.getMethod() == RequestMethod.POST) {
            response = doPost(requestParam);
        }
        return response;
    }

    /**
     * @param doGetUrl       请求的URL
     * @param httpDataEntity 请求实体
     * @param contentType    contentType
     * @param requestTimeOut  超时时间
     * @return
     * @throws HttpClientUtilException
     */
    public static String doGet(String doGetUrl, HttpDataEntity httpDataEntity, MimeTypeEnum contentType,
                               int requestTimeOut, int socketTimeOut, int connectionTimeOut) throws HttpClientUtilException {

        /**
         * 创建请求的基本的对象
         */
        CloseableHttpClient httpClient = LocalHttpConnectionManager.getHttpClient();
        Map<String, Object> requestMap = httpDataEntity.getMap();

        //拼接get参数
        if (null != requestMap && requestMap.size() > 0) {
            Set<String> keySet = requestMap.keySet();
            doGetUrl += "?";
            for (String key : keySet) {
                Object value = requestMap.get(key);
                doGetUrl += key + "=" + value + "&";
            }
        }

        RequestConfig.Builder builder = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD);
        Integer timeout=null;
        if (requestTimeOut <= 0) {
            builder.setConnectionRequestTimeout(HttpConfig.DEFAULT_REQUEST_TIMEOUT);
            timeout=HttpConfig.DEFAULT_REQUEST_TIMEOUT;
        } else {
            builder.setConnectionRequestTimeout(requestTimeOut);
            timeout=requestTimeOut;
        }
        if (socketTimeOut <= 0) {
            builder.setSocketTimeout(HttpConfig.DEFAULT_REQUEST_TIMEOUT);
        } else {
            builder.setSocketTimeout(socketTimeOut);
        }
        if (connectionTimeOut <= 0) {
            builder.setConnectTimeout(HttpConfig.DEFAULT_REQUEST_TIMEOUT);
        } else {
            builder.setConnectTimeout(connectionTimeOut);
        }

        RequestConfig requestConfig = builder.build();
        HttpGet httpGet = new HttpGet(doGetUrl);
        httpGet.setConfig(requestConfig);
        httpGet.addHeader("Accept-Charset", "UTF-8");
        httpGet.setHeader("User-Agent",
            "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.118"
                + " Safari/537.36");
        if (null == contentType) {
            httpGet.addHeader("Content-Type", MimeTypeEnum.APPLICATION_FORM_URLENCODED.getMimeType());
        } else {
            httpGet.addHeader("Content-Type", contentType.getMimeType());
        }

        CloseableHttpResponse httpResponse = null;
        Long startTime=System.currentTimeMillis();
        try {
            httpResponse = httpClient.execute(httpGet);
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // 获取服务端返回的数据,并返回
                return EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            }else{
            	
            	httpGet.abort();
            }
        } catch (Exception e) {
        	
        	httpGet.abort();
        	
        	if(!"Connection reset".equals(e.getMessage())){
                Long endTime=System.currentTimeMillis();
        		LOG.error("Failed in requesting " + doGetUrl + " data. error {}:{} times {} timeout {}", e.getClass(), e.getMessage(),endTime-startTime,timeout);
        	}
        } finally {
        	
        	if(httpClient != null){
        		
        		try {
        			httpClient.getConnectionManager().closeExpiredConnections();
        		} catch (Exception e1) {
        			// TODO Auto-generated catch block
        			e1.printStackTrace();
        		}
        	}
        	
            if (httpResponse != null) {
                try {
//                    httpResponse.close();
                	httpResponse.getEntity().getContent().close();
                } catch (IOException e) {
                    LOG.error("close http stream failed, error:{}", e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * @param requestParam 请求的参数对象
     * @return
     * @throws HttpClientUtilException
     */
    public static String doGet(RequestParam requestParam) throws HttpClientUtilException {
        return doGet(requestParam.getUrl(), requestParam.getHttpDataEntity(), requestParam.getContentType(),
            requestParam.getRequestTimeOut(), requestParam.getSocketTimeOut(), requestParam.getConnectionTimeOut());
    }

    public static String doGet(String doGetUrl) throws HttpClientUtilException {
        return doGet(doGetUrl, HttpDataEntity.createHttpDataEntity(), MimeTypeEnum.APPLICATION_FORM_URLENCODED,
            HttpConfig.DEFAULT_REQUEST_TIMEOUT, HttpConfig.DEFAULT_SOCKET_TIMEOUT,
            HttpConfig.DEFAULT_CONNECTION_TIMEOUT);
    }

    /**
     * @param doGetUrl 请求的URL
     * @param timeOut  超时时间
     * @return
     * @throws HttpClientUtilException
     */
    public static String doGet(String doGetUrl, int timeOut) throws HttpClientUtilException {
        return doGet(doGetUrl, HttpDataEntity.createHttpDataEntity(), MimeTypeEnum.APPLICATION_FORM_URLENCODED, timeOut,
            HttpConfig.DEFAULT_SOCKET_TIMEOUT, HttpConfig.DEFAULT_CONNECTION_TIMEOUT);
    }

    /**
     * @param doGetUrl       请求的URL
     * @param httpDataEntity 请求实体
     * @return
     * @throws HttpClientUtilException
     */
    public static String doGet(String doGetUrl, HttpDataEntity httpDataEntity) throws HttpClientUtilException {
        return doGet(doGetUrl, httpDataEntity, MimeTypeEnum.APPLICATION_FORM_URLENCODED,
            HttpConfig.DEFAULT_REQUEST_TIMEOUT, HttpConfig.DEFAULT_SOCKET_TIMEOUT,
            HttpConfig.DEFAULT_CONNECTION_TIMEOUT);
    }

    /**
     * @param doGetUrl       请求的URL
     * @param httpDataEntity 请求实体
     * @param timeOut        超时时间
     * @return
     * @throws HttpClientUtilException
     */
    public static String doGet(String doGetUrl, HttpDataEntity httpDataEntity, int timeOut)
        throws HttpClientUtilException {
        return doGet(doGetUrl, httpDataEntity, MimeTypeEnum.APPLICATION_FORM_URLENCODED, timeOut,
            HttpConfig.DEFAULT_SOCKET_TIMEOUT, HttpConfig.DEFAULT_CONNECTION_TIMEOUT);
    }

    /**
     * @param doPostUrl      请求的URL
     * @param httpDataEntity 请求实体
     * @param contentType    contentType
     * @param requestTimeOut        超时时间
     * @return
     * @throws HttpClientUtilException
     */
    public static String doPost(String doPostUrl, HttpDataEntity httpDataEntity, MimeTypeEnum contentType,
        int requestTimeOut, int socketTimeOut, int connectionTimeOut) throws HttpClientUtilException {
        /**
         * 创建 请求的对象
         */
        CloseableHttpClient httpClient = LocalHttpConnectionManager.getHttpClient();
        RequestConfig.Builder builder = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD);
        if (requestTimeOut <= 0) {
            builder.setConnectionRequestTimeout(HttpConfig.DEFAULT_REQUEST_TIMEOUT);
        } else {
            builder.setConnectionRequestTimeout(requestTimeOut);
        }
        if (socketTimeOut <= 0) {
            builder.setSocketTimeout(HttpConfig.DEFAULT_REQUEST_TIMEOUT);
        } else {
            builder.setSocketTimeout(socketTimeOut);
        }
        if (connectionTimeOut <= 0) {
            builder.setConnectTimeout(HttpConfig.DEFAULT_REQUEST_TIMEOUT);
        } else {
            builder.setConnectTimeout(connectionTimeOut);
        }

        RequestConfig requestConfig = builder.build();

        /**
         * 设置请求的post
         */
        HttpPost httpPost = new HttpPost(doPostUrl);
        httpPost.setConfig(requestConfig);

        /**
         * 请求的参数
         */
        List<NameValuePair> params = new ArrayList<NameValuePair>(5);
        Map<String, Object> map = httpDataEntity.getMap();
        if (null != map && map.size() > 0) {
            Set<String> keys = map.keySet();
            for (String key : keys) {
                Object value = map.get(key);
                params.add(new BasicNameValuePair(key, String.valueOf(value)));
            }
        }

        if (params.size() > 0) {
            try {
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, "UTF-8");
                /**
                 * 设置请求的数据
                 */
                httpPost.setEntity(entity);
            } catch (UnsupportedEncodingException e) {
                LOG.error("Failed in set " + doPostUrl + " params. {}", e.getMessage());
            }

        }

        httpPost.setHeader("User-Agent",
            "Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.118"
                + " Safari/537.36");
        if (null != contentType && !"".equals(contentType)) {
            httpPost.addHeader("Content-Type", contentType.getMimeType());
        } else {
            httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
        }
        httpPost.addHeader("Connection", "Keep-Alive");

        CloseableHttpResponse httpResponse = null;
        try {
        	
            httpResponse = httpClient.execute(httpPost);
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // 获取服务端返回的数据,并返回
                return EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            }else{
            	
            	httpPost.abort();
            }
        } catch (Exception e) {
        	
        	httpPost.abort();
        	
        	if(!"Connection reset".equals(e.getMessage())){
        		
        		LOG.error("Failed in requesting " + doPostUrl + " data. error {}:{}", e.getClass(), e.getMessage());
        	}
        } finally {
        	
        	if(httpClient != null){
        		
        		try {
        			httpClient.getConnectionManager().closeExpiredConnections();
        		} catch (Exception e1) {
        			// TODO Auto-generated catch block
        			e1.printStackTrace();
        		}
        	}
            if (httpResponse != null) {
                try {
//                    httpResponse.close();
                	httpResponse.getEntity().getContent().close();
                } catch (IOException e) {
                    LOG.error("close http stream failed, error:{}", e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * @param requestParam 请求的参数对象
     * @return
     * @throws HttpClientUtilException
     */
    public static String doPost(RequestParam requestParam) throws HttpClientUtilException {
        return doPost(requestParam.getUrl(), requestParam.getHttpDataEntity(), requestParam.getContentType(),
            requestParam.getRequestTimeOut(), requestParam.getSocketTimeOut(), requestParam.getConnectionTimeOut());
    }

    /**
     * @param doPostUrl 请求的URL
     * @return
     * @throws HttpClientUtilException
     */
    public static String doPost(String doPostUrl) throws HttpClientUtilException {
        return doPost(doPostUrl, HttpDataEntity.createHttpDataEntity(), MimeTypeEnum.APPLICATION_FORM_URLENCODED,
            HttpConfig.DEFAULT_REQUEST_TIMEOUT, HttpConfig.DEFAULT_SOCKET_TIMEOUT,
            HttpConfig.DEFAULT_CONNECTION_TIMEOUT);
    }

    /**
     * @param doPostUrl 请求的URL
     * @param timeOut   超时时间
     * @return
     * @throws HttpClientUtilException
     */
    public static String doPost(String doPostUrl, int timeOut) throws HttpClientUtilException {
        return doPost(doPostUrl, HttpDataEntity.createHttpDataEntity(), MimeTypeEnum.APPLICATION_FORM_URLENCODED,
            timeOut, HttpConfig.DEFAULT_SOCKET_TIMEOUT, HttpConfig.DEFAULT_CONNECTION_TIMEOUT);
    }

    /**
     * @param doPostUrl   请求的URL
     * @param contentType contentType
     * @return
     * @throws HttpClientUtilException
     */
    public static String doPost(String doPostUrl, MimeTypeEnum contentType) throws HttpClientUtilException {
        return doPost(doPostUrl, HttpDataEntity.createHttpDataEntity(), contentType, HttpConfig.DEFAULT_REQUEST_TIMEOUT,
            HttpConfig.DEFAULT_SOCKET_TIMEOUT, HttpConfig.DEFAULT_CONNECTION_TIMEOUT);
    }

    /**
     * @param doPostUrl   请求的URL
     * @param contentType contentType
     * @param timeOut     超时时间
     * @return
     * @throws HttpClientUtilException
     */
    public static String doPost(String doPostUrl, MimeTypeEnum contentType, int timeOut)
        throws HttpClientUtilException {
        return doPost(doPostUrl, HttpDataEntity.createHttpDataEntity(), contentType, timeOut,
            HttpConfig.DEFAULT_SOCKET_TIMEOUT, HttpConfig.DEFAULT_CONNECTION_TIMEOUT);
    }

    /**
     * @param doPostUrl      请求的URL
     * @param httpDataEntity 请求实体
     * @return
     * @throws HttpClientUtilException
     */
    public static String doPost(String doPostUrl, HttpDataEntity httpDataEntity) throws HttpClientUtilException {
        return doPost(doPostUrl, httpDataEntity, MimeTypeEnum.APPLICATION_FORM_URLENCODED,
            HttpConfig.DEFAULT_REQUEST_TIMEOUT, HttpConfig.DEFAULT_SOCKET_TIMEOUT,
            HttpConfig.DEFAULT_CONNECTION_TIMEOUT);
    }

    /**
     * @param doPostUrl      请求的URL
     * @param httpDataEntity 请求实体
     * @param timeOut        超时时间
     * @return
     * @throws HttpClientUtilException
     */
    public static String doPost(String doPostUrl, HttpDataEntity httpDataEntity, int timeOut)
        throws HttpClientUtilException {
        return doPost(doPostUrl, httpDataEntity, MimeTypeEnum.APPLICATION_FORM_URLENCODED, timeOut,
            HttpConfig.DEFAULT_SOCKET_TIMEOUT, HttpConfig.DEFAULT_CONNECTION_TIMEOUT);
    }

    /**
     * @param doPostUrl      请求的URL
     * @param httpDataEntity 请求实体
     * @param contentType    contentType
     * @return
     * @throws HttpClientUtilException
     */
    public static String doPost(String doPostUrl, HttpDataEntity httpDataEntity, MimeTypeEnum contentType)
        throws HttpClientUtilException {
        return doPost(doPostUrl, httpDataEntity, contentType, HttpConfig.DEFAULT_REQUEST_TIMEOUT,
            HttpConfig.DEFAULT_SOCKET_TIMEOUT, HttpConfig.DEFAULT_CONNECTION_TIMEOUT);
    }

}
