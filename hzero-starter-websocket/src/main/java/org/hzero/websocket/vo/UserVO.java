package org.hzero.websocket.vo;

/**
 * description
 *
 * @author shuangfei.zhu@hand-china.com 2019/06/14 9:45
 */
public class UserVO {

    /**
     * websocketSession id
     */
    private String sessionId;
    /**
     * 当前租户
     */
    private Long tenantId;
    /**
     * 当前角色
     */
    private Long roleId;
    /**
     * token
     */
    private String accessToken;
    /**
     * brokerId
     */
    private String brokerId;

    public UserVO() {

    }

    public UserVO(String sessionId, Long tenantId, Long roleId, String accessToken, String brokerId) {
        this.sessionId = sessionId;
        this.tenantId = tenantId;
        this.roleId = roleId;
        this.accessToken = accessToken;
        this.brokerId = brokerId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public UserVO setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public UserVO setTenantId(Long tenantId) {
        this.tenantId = tenantId;
        return this;
    }

    public Long getRoleId() {
        return roleId;
    }

    public UserVO setRoleId(Long roleId) {
        this.roleId = roleId;
        return this;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public UserVO setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public String getBrokerId() {
        return brokerId;
    }

    public UserVO setBrokerId(String brokerId) {
        this.brokerId = brokerId;
        return this;
    }

    @Override
    public String toString() {
        return "UserVO{" +
                "sessionId='" + sessionId + '\'' +
                ", tenantId=" + tenantId +
                ", roleId=" + roleId +
                ", accessToken='" + accessToken + '\'' +
                ", brokerId='" + brokerId + '\'' +
                '}';
    }
}
