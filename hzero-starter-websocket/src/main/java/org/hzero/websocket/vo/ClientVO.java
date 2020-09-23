package org.hzero.websocket.vo;

/**
 * description
 *
 * @author shuangfei.zhu@hand-china.com 2020/04/22 15:24
 */
public class ClientVO {

    public ClientVO() {
    }

    public ClientVO(String sessionId, String group, String brokerId) {
        this.sessionId = sessionId;
        this.group = group;
        this.brokerId = brokerId;
    }

    /**
     * websocketSession id
     */
    private String sessionId;
    /**
     * group
     */
    private String group;
    /**
     * brokerId
     */
    private String brokerId;

    public String getSessionId() {
        return sessionId;
    }

    public ClientVO setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public String getGroup() {
        return group;
    }

    public ClientVO setGroup(String group) {
        this.group = group;
        return this;
    }

    public String getBrokerId() {
        return brokerId;
    }

    public ClientVO setBrokerId(String brokerId) {
        this.brokerId = brokerId;
        return this;
    }

    @Override
    public String toString() {
        return "ServerVO{" +
                "sessionId='" + sessionId + '\'' +
                ", group='" + group + '\'' +
                ", brokerId='" + brokerId + '\'' +
                '}';
    }
}
