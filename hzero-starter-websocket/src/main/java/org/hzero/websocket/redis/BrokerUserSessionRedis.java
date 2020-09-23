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
import org.hzero.websocket.vo.UserVO;
import org.springframework.context.ApplicationContext;

import io.choerodon.core.convertor.ApplicationContextHelper;

/**
 * redis存储userId-sessionId
 *
 * @author shuangfei.zhu@hand-china.com 2018/11/13 17:12
 */
public class BrokerUserSessionRedis {

    private static RedisHelper redisHelper;

    private BrokerUserSessionRedis() {
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
        return WebSocketConstant.REDIS_KEY + ":broker-user-sessions:" + brokerId;
    }

    /**
     * 刷新缓存
     *
     * @param user 用户信息
     */
    public static void refreshCache(String brokerId, Long userId, UserVO user) {
        List<UserVO> userInfoList = getCache(brokerId, userId);
        List<String> sessionIds = userInfoList.stream().map(UserVO::getSessionId).collect(Collectors.toList());
        if (!sessionIds.contains(user.getSessionId())) {
            userInfoList.add(user);
        }
        if (CollectionUtils.isEmpty(userInfoList)) {
            clearRedisCache(getRedisHelper(), brokerId, userId);
        } else {
            getRedisHelper().hshPut(getCacheKey(brokerId), String.valueOf(userId), getRedisHelper().toJson(userInfoList));
        }
        clear();
    }

    /**
     * 刪除缓存
     *
     * @param sessionId webSocketSession  uuid
     */
    public static UserVO deleteCache(String brokerId, Long userId, String sessionId) {
        List<UserVO> userInfoList = getCache(brokerId, userId);
        UserVO invalidUser = null;
        for (UserVO item : userInfoList) {
            if (Objects.equals(item.getSessionId(), sessionId)) {
                invalidUser = item;
            }
        }
        userInfoList.remove(invalidUser);
        if (CollectionUtils.isEmpty(userInfoList)) {
            clearRedisCache(getRedisHelper(), brokerId, userId);
        } else {
            getRedisHelper().hshPut(getCacheKey(brokerId), String.valueOf(userId), getRedisHelper().toJson(userInfoList));
        }
        clear();
        return invalidUser;
    }

    /**
     * 查询缓存
     *
     * @param brokerId 服务Id
     */
    public static List<UserVO> getCache(String brokerId, Long userId) {
        List<UserVO> result = new ArrayList<>();
        String sessionIds = getRedisHelper().hshGet(getCacheKey(brokerId), String.valueOf(userId));
        if (StringUtils.isNotBlank(sessionIds)) {
            result = getRedisHelper().fromJsonList(sessionIds, UserVO.class);
        }
        clear();
        return result;
    }

    /**
     * 查询缓存
     *
     * @param brokerId 服务Id
     */
    public static List<UserVO> getCache(String brokerId) {
        List<UserVO> result = new ArrayList<>();
        Map<String, String> map = getRedisHelper().hshGetAll(getCacheKey(brokerId));
        map.forEach((k, v) -> {
            if (StringUtils.isNotBlank(v)) {
                result.addAll(getRedisHelper().fromJsonList(v, UserVO.class));
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
    private static void clearRedisCache(RedisHelper redisHelper, String brokerId, Long userId) {
        redisHelper.hshDelete(getCacheKey(brokerId), String.valueOf(userId));
        clear();
    }

    /**
     * 清空服务缓存
     *
     * @param brokerId 服务Id
     */
    public static void clearRedisCacheByBrokerId(String brokerId) {
        RedisHelper redisHelper = getRedisHelper();
        Map<String, String> map = redisHelper.hshGetAll(getCacheKey(brokerId));
        // 清理在线用户缓存
        map.forEach((userId, users) -> SessionUserRedis.deleteCache(redisHelper.fromJsonList(users, UserVO.class)));
        getRedisHelper().delKey(getCacheKey(brokerId));
        clear();
    }
}
