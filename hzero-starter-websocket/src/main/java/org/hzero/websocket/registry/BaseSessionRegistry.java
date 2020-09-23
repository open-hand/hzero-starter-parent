package org.hzero.websocket.registry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.hzero.core.util.UUIDUtils;
import org.hzero.websocket.redis.BrokerServerSessionRedis;
import org.hzero.websocket.redis.BrokerUserSessionRedis;
import org.hzero.websocket.redis.SessionUserRedis;
import org.hzero.websocket.vo.UserVO;

/**
 * description
 *
 * @author shuangfei.zhu@hand-china.com 2020/04/22 10:55
 */
public abstract class BaseSessionRegistry {

    protected BaseSessionRegistry() {
    }

    private static final String BROKER_ID = UUIDUtils.generateUUID();

    public static String getBrokerId() {
        return BROKER_ID;
    }

    private static final Lock lock = new ReentrantLock();

    /**
     * 清理session内存及缓存
     *
     * @param sessionId sessionId
     */
    public static void clearSession(String sessionId) {
        Long userId = UserSessionRegistry.getUser(sessionId);
        try {
            lock.tryLock(10, TimeUnit.SECONDS);
            if (userId != null) {
                // 清理Broker-Session
                UserVO invalidUser = BrokerUserSessionRedis.deleteCache(BROKER_ID, userId, sessionId);
                // 清理Session-User
                SessionUserRedis.deleteCache(invalidUser);
                // 清理内存
                UserSessionRegistry.removeSession(sessionId);
            }
            String group = GroupSessionRegistry.getGroup(sessionId);
            if (group != null) {
                // 清理Broker-Session
                BrokerServerSessionRedis.deleteCache(BROKER_ID, group, sessionId);
                // 清理内存
                GroupSessionRegistry.removeSession(sessionId);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }
}
