package org.hzero.websocket.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections4.MapUtils;
import org.springframework.web.socket.WebSocketSession;

/**
 * 用户WebSocketSession存储
 *
 * @author shuangfei.zhu@hand-china.com 2019/04/18 17:19
 */
public class UserSessionRegistry extends BaseSessionRegistry {

    private UserSessionRegistry() {
    }

    /**
     * 内存存储webSocketSession  sessionId webSocketSession
     */
    private static final Map<String, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 内存存储webSocketSession  sessionId userId
     */
    private static final Map<String, Long> USER_MAP = new ConcurrentHashMap<>();

    /**
     * 添加session存储
     */
    public static void addSession(WebSocketSession session, String sessionId, Long userId) {
        SESSION_MAP.put(sessionId, session);
        USER_MAP.put(sessionId, userId);
    }

    /**
     * 移除session存储
     */
    public static void removeSession(String sessionId) {
        // 移除sessionId webSocketSession
        SESSION_MAP.remove(sessionId);
        USER_MAP.remove(sessionId);
    }

    /**
     * 获取WebSocketSession
     */
    public static WebSocketSession getSession(String sessionId) {
        if (MapUtils.isEmpty(SESSION_MAP)) {
            return null;
        }
        return SESSION_MAP.get(sessionId);
    }

    /**
     * 获取userId
     */
    public static Long getUser(String sessionId) {
        if (MapUtils.isEmpty(USER_MAP)) {
            return null;
        }
        return USER_MAP.get(sessionId);
    }
}
