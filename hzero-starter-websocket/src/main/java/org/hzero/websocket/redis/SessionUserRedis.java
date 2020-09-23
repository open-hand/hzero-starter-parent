package org.hzero.websocket.redis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hzero.core.redis.RedisHelper;
import org.hzero.websocket.config.WebSocketConfig;
import org.hzero.websocket.constant.WebSocketConstant;
import org.hzero.websocket.vo.UserVO;
import org.springframework.context.ApplicationContext;

import io.choerodon.core.convertor.ApplicationContextHelper;

/**
 * 在线用户缓存工具
 *
 * @author shuangfei.zhu@hand-china.com 2019/10/11 20:19
 */
@SuppressWarnings("unused")
public class SessionUserRedis {

    private static RedisHelper redisHelper;

    private SessionUserRedis() {
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
        return WebSocketConstant.REDIS_KEY + ":session-users";
    }

    /**
     * 生成redis存储key
     *
     * @return key
     */
    private static String getCacheKey(Long tenantId) {
        return WebSocketConstant.REDIS_KEY + ":session-users:" + tenantId;
    }

    /**
     * 刷新缓存
     *
     * @param user 用户信息
     */
    public static void refreshCache(UserVO user) {
        RedisHelper redisHelper = getRedisHelper();
        // 缓存记录sessionId 与用户的关系，还要考虑分页排序
        long date = System.nanoTime();
        redisHelper.zSetAdd(getCacheKey(), redisHelper.toJson(user), date);
        redisHelper.zSetAdd(getCacheKey(user.getTenantId()), redisHelper.toJson(user), date);
        clear();
    }

    /**
     * 刪除缓存
     *
     * @param user 用户信息
     */
    public static void deleteCache(UserVO user) {
        RedisHelper redisHelper = getRedisHelper();
        redisHelper.zSetRemove(getCacheKey(), redisHelper.toJson(user));
        redisHelper.zSetRemove(getCacheKey(user.getTenantId()), redisHelper.toJson(user));
        clear();
    }

    /**
     * 刪除缓存
     *
     * @param userList 用户信息
     */
    public static void deleteCache(List<UserVO> userList) {
        RedisHelper redisHelper = getRedisHelper();
        userList.forEach(user -> redisHelper.zSetRemove(getCacheKey(), redisHelper.toJson(user)));
        userList.forEach(user -> redisHelper.zSetRemove(getCacheKey(user.getTenantId()), redisHelper.toJson(user)));
        clear();
    }

    /**
     * 获取在线人数
     */
    public static Long getSize() {
        RedisHelper redis = getRedisHelper();
        Long total = redis.zSetSize(getCacheKey());
        clear();
        return total;
    }

    /**
     * 指定租户获取在线人数
     */
    public static Long getSize(Long tenantId) {
        RedisHelper redis = getRedisHelper();
        Long total = redis.zSetSize(getCacheKey(tenantId));
        clear();
        return total;
    }

    /**
     * 分页查询在线用户
     *
     * @param page 页
     * @param size 每页数量
     */
    public static List<UserVO> getCache(int page, int size) {
        RedisHelper redis = getRedisHelper();
        int start = size * page;
        int end = start + size - 1;
        Set<String> keySets = redis.zSetRange(getCacheKey(), (long) start, (long) end);
        clear();
        List<UserVO> list = new ArrayList<>();
        keySets.forEach(item -> list.add(redis.fromJson(item, UserVO.class)));
        return list;
    }

    /**
     * 分页查询在线用户
     *
     * @param page 页
     * @param size 每页数量
     */
    public static List<UserVO> getCache(int page, int size, long tenantId) {
        RedisHelper redis = getRedisHelper();
        int start = size * page;
        int end = start + size - 1;
        Set<String> keySets = redis.zSetRange(getCacheKey(tenantId), (long) start, (long) end);
        clear();
        List<UserVO> list = new ArrayList<>();
        keySets.forEach(item -> list.add(redis.fromJson(item, UserVO.class)));
        return list;
    }

    /**
     * 查询所有在线用户
     */
    public static List<UserVO> getCache() {
        RedisHelper redis = getRedisHelper();
        Long count = redis.zSetSize(getCacheKey());
        Set<String> keySets = redis.zSetRange(getCacheKey(), 0L, count);
        clear();
        List<UserVO> list = new ArrayList<>();
        keySets.forEach(item -> list.add(redis.fromJson(item, UserVO.class)));
        return list;
    }

    /**
     * 指定租户查询所有在线用户
     */
    public static List<UserVO> getCache(Long tenantId) {
        RedisHelper redis = getRedisHelper();
        Long count = redis.zSetSize(getCacheKey(tenantId));
        Set<String> keySets = redis.zSetRange(getCacheKey(tenantId), 0L, count);
        clear();
        List<UserVO> list = new ArrayList<>();
        keySets.forEach(item -> list.add(redis.fromJson(item, UserVO.class)));
        return list;
    }
}
