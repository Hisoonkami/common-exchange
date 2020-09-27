package com.adev.common.exchange.http.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author: xianninig
 * @date: 2018/8/29 16:23
 */
public class HttpDataEntity implements Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(HttpDataEntity.class);

    private static final String CHINESE_PATTERN = "[\\u4e00-\\u9fa5]";

    private static final Pattern CHINESE_PATTERN_COMPILED = Pattern.compile(CHINESE_PATTERN);
    /**
     * 初始化请求的参数
     */
    private Map<String, Object> map = new HashMap<>(1 << 4);


    /**
     * 避免创建对象
     */
    private HttpDataEntity() {
    }

    /**
     * @param map
     */
    private HttpDataEntity(Map<String, Object> map) {
        this.map = map;
    }


    /**
     * 只能得到 不能设置
     *
     * @return Map
     */
    public Map<String, Object> getMap() {
        return map;
    }

    /**
     * 创建无参数的请求的对象
     *
     * @return HttpDataEntity
     */
    public static HttpDataEntity createHttpDataEntity() {
        return new HttpDataEntity();
    }

    /**
     * @param keyValues 请求的参数
     *                  如果有多个参数  用 逗号 分号 取址符 空格  换行\r\n |  \n    进行隔开即可    分隔开
     *                  eg: name=zlg  ||  name=zlg&age=23  name=zlg,age=23&realname=zzz;hhh=jjj          hshsh=uuu
     * @return {"name":"zlg","hhh":"jjj","hshsh":"uuu","age":"23","realname":"zzz"}
     */
    public static HttpDataEntity createHttpDataEntity(String keyValues) {

        Map<String, Object> map = new HashMap<>();

        if (null == keyValues) {
            return new HttpDataEntity();
        }

        if ("".equals(keyValues)) {
            return new HttpDataEntity();
        }

        /**
         * 去掉多余的空格
         */
        keyValues = keyValues.replaceAll(" +", " ");
        String[] split = keyValues.split("[&| +|;|,|\r\n|\n]");
        return getHttpDataEntity(map, split);

    }

    public static void main(String[] args) {
        System.out.println(createHttpDataEntity("username=zhangsan&age=33").getMap());
    }
    /**
     * @param keyValue 请求的参数 eg: name=zlg
     * @return
     */
    public static HttpDataEntity createHttpDataEntity(String... keyValue) {

        Map<String, Object> map = new HashMap<>();
        if (null == keyValue) {
            return new HttpDataEntity();
        }
        return getHttpDataEntity(map, keyValue);
    }


    private static HttpDataEntity getHttpDataEntity(Map<String, Object> map, String[] keyValue) {
        if (null != keyValue && keyValue.length > 0) {
            for (String str : keyValue) {
                if (null != str && str.length() > 0 && str.indexOf('=') > 0) {
                    String[] agrs = str.split("=");
                    if (null == agrs || 0 == agrs.length) {
                        throw new IllegalArgumentException("请求的参数错误----含有没有写nam与value的请求参数，已忽略");
                    } else {
                        if (null == agrs[0] || "".equals(agrs[0])) {
                            throw new IllegalArgumentException("请求的参数错误----含有没有写nam的请求参数，已忽略");
                        } else {
                            String key = agrs[0];
                            String value = agrs.length == 2 ? agrs[1] : "";
                            if (isHaveChinese(value)) {
                                try {
                                    value = URLEncoder.encode(value, "Utf-8");
                                } catch (UnsupportedEncodingException e) {
                                    LOG.error("发生错误", e);
                                }
                                map.put(key, value);
                            } else {
                                map.put(key, value);
                            }

                        }
                    }
                } else {
                    throw new IllegalArgumentException("请求的参数错误---请求的参数的格式错误");
                }

            }
        } else {
            throw new IllegalArgumentException("请求的参数错误");
        }

        return new HttpDataEntity(map);
    }


    /**
     * 判断里面是否含有汉字
     *
     * @param entityFieldValue
     * @return
     */
    private static boolean isHaveChinese(String entityFieldValue) {
        Matcher matcher = CHINESE_PATTERN_COMPILED.matcher(entityFieldValue);
        if (matcher.find()) {
            return false;
        }
        return false;
    }

}
