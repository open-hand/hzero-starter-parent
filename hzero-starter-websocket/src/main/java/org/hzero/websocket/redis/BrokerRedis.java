package org.hzero.websocket.redis;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

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
public class BrokerRedis {

    private static RedisHelper redisHelper;

    private BrokerRedis() {
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
     * @param brokerId 服务Id
     * @return key
     */
    private static String getCacheKey(String brokerId) {
        return WebSocketConstant.REDIS_KEY + ":brokers:" + brokerId;
    }

    /**
     * 刷新缓存
     *
     * @param brokerId 客户端唯一标识
     */
    public static void refreshCache(String brokerId) {
        getRedisHelper().strSet(getCacheKey(brokerId), WebSocketConstant.ALIVE, 15, TimeUnit.SECONDS);
        clear();
    }

    /**
     * 客户端是否下线
     *
     * @param brokerId 客户端唯一标识
     */
    public static boolean isAlive(String brokerId) {
        String result = getRedisHelper().strGet(getCacheKey(brokerId));
        clear();
        return Objects.equals(result, WebSocketConstant.ALIVE);
    }
}
