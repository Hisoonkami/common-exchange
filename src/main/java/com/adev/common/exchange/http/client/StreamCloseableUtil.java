package com.adev.common.exchange.http.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author: xianninig
 * @date: 2018/8/29 16:36
 */
public class StreamCloseableUtil {
    private static final Logger LOG = LoggerFactory.getLogger(StreamCloseableUtil.class);

    /**
     * 统一关闭文件的流操作
     *
     * @param closeables
     */
    public static void closeStream(Closeable... closeables) {
        if (null != closeables && closeables.length > 0) {
            for (int i = 0; i < closeables.length; i++) {
                if (null != closeables[i]) {
                    try {
                        closeables[i].close();
                    } catch (IOException e) {
                        LOG.error("关闭流失败");
                    }
                }
            }
        }
    }
}
