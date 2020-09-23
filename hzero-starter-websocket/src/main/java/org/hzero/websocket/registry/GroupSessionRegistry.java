package org.hzero.websocket.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.collections4.MapUtils;
import org.springframework.web.socket.WebSocketSession;

/**
 * 密钥链接WebSocketSession存储
 *
 * @author shuangfei.zhu@hand-china.com 2020/04/22 10:54
 */
public class GroupSessionRegistry extends BaseSessionRegistry {

    private GroupSessionRegistry() {
    }

    /**
     * 内存存储webSocketSession  sessionId webSocketSession
     */
    private static final Map<String, WebSocketSession> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * 内存存储webSocketSession  sessionId group
     */
    private static final Map<String, String> GROUP_MAP = new ConcurrentHashMap<>();

    /**
     * 添加session存储
     */
    public static void addSession(WebSocketSession session, String sessionId, String group) {
        SESSION_MAP.put(sessionId, session);
        GROUP_MAP.put(sessionId, group);
    }

    /**
     * 移除session存储
     */
    public static void removeSession(String sessionId) {
        // 移除sessionId webSocketSession
        SESSION_MAP.remove(sessionId);
        GROUP_MAP.remove(sessionId);
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
     * 获取group
     */
    public static String getGroup(String sessionId) {
        if (MapUtils.isEmpty(GROUP_MAP)) {
            return null;
        }
        return GROUP_MAP.get(sessionId);
    }
}
