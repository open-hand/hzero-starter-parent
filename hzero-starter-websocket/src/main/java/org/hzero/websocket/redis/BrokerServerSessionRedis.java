package org.hzero.websocket.redis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hzero.core.redis.RedisHelper;
import org.hzero.websocket.config.WebSocketConfig;
import org.hzero.websocket.constant.WebSocketConstant;
import org.hzero.websocket.vo.ClientVO;
import org.springframework.context.ApplicationContext;

import io.choerodon.core.convertor.ApplicationContextHelper;

/**
 * description
 *
 * @author shuangfei.zhu@hand-china.com 2020/04/22 15:23
 */
public class BrokerServerSessionRedis {
    private static RedisHelper redisHelper;

    private BrokerServerSessionRedis() {
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
        return WebSocketConstant.REDIS_KEY + ":broker-server-sessions:" + brokerId;
    }

    /**
     * 刷新缓存
     *
     * @param server 服务信息
     */
    public static void refreshCache(String brokerId, String group, ClientVO server) {
        List<ClientVO> serverList = getCache(brokerId, group);
        List<String> sessionIds = serverList.stream().map(ClientVO::getSessionId).collect(Collectors.toList());
        if (!sessionIds.contains(server.getSessionId())) {
            serverList.add(server);
        }
        if (CollectionUtils.isEmpty(serverList)) {
            clearRedisCache(getRedisHelper(), brokerId, group);
        } else {
            getRedisHelper().hshPut(getCacheKey(brokerId), group, getRedisHelper().toJson(serverList));
        }
        clear();
    }

    /**
     * 刪除缓存
     *
     * @param sessionId webSocketSession  uuid
     */
    public static ClientVO deleteCache(String brokerId, String group, String sessionId) {
        List<ClientVO> serverList = getCache(brokerId, group);
        ClientVO invalidServer = null;
        for (ClientVO item : serverList) {
            if (Objects.equals(item.getSessionId(), sessionId)) {
                invalidServer = item;
            }
        }
        serverList.remove(invalidServer);
        if (CollectionUtils.isEmpty(serverList)) {
            clearRedisCache(getRedisHelper(), brokerId, group);
        } else {
            getRedisHelper().hshPut(getCacheKey(brokerId), group, getRedisHelper().toJson(serverList));
        }
        clear();
        return invalidServer;
    }

    /**
     * 查询缓存
     *
     * @param brokerId 服务Id
     */
    public static List<ClientVO> getCache(String brokerId, String group) {
        List<ClientVO> result = new ArrayList<>();
        String sessionIds = getRedisHelper().hshGet(getCacheKey(brokerId), group);
        if (StringUtils.isNotBlank(sessionIds)) {
            result = getRedisHelper().fromJsonList(sessionIds, ClientVO.class);
        }
        clear();
        return result;
    }

    /**
     * 查询缓存
     *
     * @param brokerId 服务Id
     */
    public static List<ClientVO> getCache(String brokerId) {
        List<ClientVO> result = new ArrayList<>();
        Map<String, String> map = getRedisHelper().hshGetAll(getCacheKey(brokerId));
        map.forEach((k, v) -> {
            if (StringUtils.isNotBlank(v)) {
                result.addAll(getRedisHelper().fromJsonList(v, ClientVO.class));
            }
        });
        clear();
        return result;
    }

    /**
     * 清空缓存
     *
     * @param redisHelper redis
     * @param brokerId    服务Id
     */
    private static void clearRedisCache(RedisHelper redisHelper, String brokerId, String group) {
        redisHelper.hshDelete(getCacheKey(brokerId), group);
        clear();
    }

    /**
     * 清空服务缓存
     *
     * @param brokerId 服务Id
     */
    public static void clearRedisCacheByBrokerId(String brokerId) {
        getRedisHelper().delKey(getCacheKey(brokerId));
        clear();
    }
}
