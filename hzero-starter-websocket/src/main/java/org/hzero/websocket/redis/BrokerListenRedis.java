package org.hzero.websocket.redis;

import java.util.*;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hzero.core.redis.RedisHelper;
import org.hzero.websocket.config.WebSocketConfig;
import org.hzero.websocket.constant.WebSocketConstant;
import org.springframework.context.ApplicationContext;

import io.choerodon.core.convertor.ApplicationContextHelper;

/**
 * 客户端心跳缓存
 *
 * @author shuangfei.zhu@hand-china.com 2019/05/30 15:17
 */
public class BrokerListenRedis {

    private static RedisHelper redisHelper;

    private BrokerListenRedis() {
    }

    private static RedisHelper getRedisHelper() {
        ApplicationContext applicationContext = ApplicationContextHelper.getContext();
        if (redisHelper == null) {
            redisHelper = applicationContext.getBean(RedisHelper.class);
        }
        redisHelper.setCurrentDatabase(applicationContext.getBean(WebSocketConfig.class).getRedisDb());
        return redisHelper;
    }

    private static void clear() {
        if (redisHelper == null) {
            redisHelper = ApplicationContextHelper.getContext().getBean(RedisHelper.class);
        }
        redisHelper.clearCurrentDatabase();
    }

    /**
     * 生成redis存储key
     *
     * @return key
     */
    private static String getCacheKey() {
        return WebSocketConstant.REDIS_KEY + ":brokers";
    }

    /**
     * 刷新缓存
     *
     * @param brokerId 客户端唯一标识
     */
    public static void refreshCache(String brokerId) {
        getRedisHelper().hshPut(getCacheKey(), brokerId, StringUtils.EMPTY);
        clear();
    }

    /**
     * 查询缓存
     */
    public static List<String> getCache() {
        Set<String> set = ObjectUtils.defaultIfNull(getRedisHelper().hshKeys(getCacheKey()), new HashSet<>());
        clear();
        return new ArrayList<>(set);
    }

    /**
     * 清空缓存
     *
     * @param brokerId    客户端唯一标识
     */
    public static void clearRedisCache(String brokerId) {
        getRedisHelper().hshDelete(getCacheKey(), brokerId);
        clear();
    }
}
