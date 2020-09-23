package org.hzero.websocket.listener;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hzero.websocket.constant.WebSocketConstant;
import org.hzero.websocket.redis.BrokerServerSessionRedis;
import org.hzero.websocket.redis.BrokerUserSessionRedis;
import org.hzero.websocket.registry.BaseSessionRegistry;
import org.hzero.websocket.registry.GroupSessionRegistry;
import org.hzero.websocket.registry.UserSessionRegistry;
import org.hzero.websocket.util.SocketSessionUtils;
import org.hzero.websocket.vo.ClientVO;
import org.hzero.websocket.vo.MsgVO;
import org.hzero.websocket.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * redis 通道消息监听
 *
 * @author shuangfei.zhu@hand-china.com 2019/04/19 15:52
 */
@Component
public class SocketMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(SocketMessageListener.class);

    private final ObjectMapper objectMapper;

    @Autowired
    public SocketMessageListener(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void messageListener(String msgVO) {
        try {
            // 监听到消息发送webSocket消息
            String brokerId = BaseSessionRegistry.getBrokerId();
            MsgVO msg = objectMapper.readValue(msgVO, MsgVO.class);
            if (Objects.equals(brokerId, msg.getBrokerId())) {
                // 消息发送方为本服务，不处理
                return;
            }
            List<String> sessionIdList;
            byte[] data = msg.getData();
            switch (msg.getType()) {
                case WebSocketConstant.SendType.SESSION:
                    WebSocketSession session = UserSessionRegistry.getSession(msg.getSessionId());
                    if (data != null) {
                        SocketSessionUtils.sendMsg(session, msg.getSessionId(), data);
                    } else {
                        SocketSessionUtils.sendMsg(session, msg.getSessionId(), msgVO);
                    }
                    break;
                case WebSocketConstant.SendType.USER:
                    List<UserVO> userList = BrokerUserSessionRedis.getCache(brokerId, msg.getUserId());
                    sessionIdList = userList.stream().map(UserVO::getSessionId).collect(Collectors.toList());
                    if (data != null) {
                        SocketSessionUtils.sendUserMsg(sessionIdList, data);
                    } else {
                        SocketSessionUtils.sendUserMsg(sessionIdList, msgVO);
                    }
                    break;
                case WebSocketConstant.SendType.ALL:
                    List<UserVO> users = BrokerUserSessionRedis.getCache(brokerId);
                    sessionIdList = users.stream().map(UserVO::getSessionId).collect(Collectors.toList());
                    if (data != null) {
                        SocketSessionUtils.sendUserMsg(sessionIdList, data);
                    } else {
                        SocketSessionUtils.sendUserMsg(sessionIdList, msgVO);
                    }
                    break;
                case WebSocketConstant.SendType.S_SESSION:
                    WebSocketSession clientSession = GroupSessionRegistry.getSession(msg.getSessionId());
                    if (data != null) {
                        SocketSessionUtils.sendMsg(clientSession, msg.getSessionId(), data);
                    } else {
                        SocketSessionUtils.sendMsg(clientSession, msg.getSessionId(), msgVO);
                    }
                    break;
                case WebSocketConstant.SendType.S_GROUP:
                    List<ClientVO> clientList = BrokerServerSessionRedis.getCache(brokerId, msg.getGroup());
                    sessionIdList = clientList.stream().map(ClientVO::getSessionId).collect(Collectors.toList());
                    if (data != null) {
                        SocketSessionUtils.sendClientMsg(sessionIdList, data);
                    } else {
                        SocketSessionUtils.sendClientMsg(sessionIdList, msgVO);
                    }
                    break;
                case WebSocketConstant.SendType.S_ALL:
                    List<ClientVO> clients = BrokerServerSessionRedis.getCache(brokerId);
                    sessionIdList = clients.stream().map(ClientVO::getSessionId).collect(Collectors.toList());
                    if (data != null) {
                        SocketSessionUtils.sendClientMsg(sessionIdList, data);
                    } else {
                        SocketSessionUtils.sendClientMsg(sessionIdList, msgVO);
                    }
                    break;
                case WebSocketConstant.SendType.CLOSE:
                    List<ClientVO> list = BrokerServerSessionRedis.getCache(brokerId, msg.getGroup());
                    sessionIdList = list.stream().map(ClientVO::getSessionId).collect(Collectors.toList());
                    // 关闭session连接
                    SocketSessionUtils.closeSession(sessionIdList);
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            logger.warn(e.getMessage());
        }
    }
}
