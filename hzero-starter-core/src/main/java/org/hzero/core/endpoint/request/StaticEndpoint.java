package org.hzero.core.endpoint.request;

import org.springframework.http.HttpMethod;

/**
 * 静态端点，系统中被认为是api不可变的端点
 * @author XCXCXCXCX
 * @date 2020/4/23 1:50 下午
 */
public enum StaticEndpoint {

    /**
     * 通用服务端点，包括拉取api文档、服务上下文刷新（配置刷新）等
     */
    CHOERODON_API_DOCS("/v2/choerodon/api-docs", HttpMethod.GET, false, "猪齿鱼原生api接口，用于拉取实例的api信息"),
    SERVICE_REFRESH("/actuator/refresh", HttpMethod.POST, true, "Spring Cloud原生api接口，用于刷新应用上下文"),
    /**
     * 配置服务端点
     */
    CONFIG_SERVICE_FETCH("/config/fetch", HttpMethod.GET, false, "HZERO配置中心拉取整个配置文件api接口"),
    CONFIG_SERVICE_PUBLISH("/config/publish", HttpMethod.POST, false, "HZERO配置中心推送整个配置文件api接口"),
    CONFIG_SERVICE_PUBLISH_KV("/config/publish-kv", HttpMethod.POST, false, "HZERO配置中心推送单个配置项api接口"),
    CONFIG_SERVICE_LISTEN("/config/listen", HttpMethod.POST, false, "HZERO配置中心监听配置项变化的api接口"),
    /**
     * 治理服务端点
     */
    ADMIN_SERVICE_REGISTER("/actuator/service-init-registry/register", HttpMethod.POST, true, "HZERO治理服务注册服务的api接口"),
    ADMIN_SERVICE_UNREGISTER("/actuator/service-init-registry/unregister", HttpMethod.POST, true, "HZERO治理服务注销服务的api接口"),
    /**
     * 网关服务端点
     */
    GATEWAY_REFRESH_METRIC_RULE("/actuator/refresh-metric-rule", HttpMethod.POST, true, "HZERO网关刷新api埋点配置接口"),
    GATEWAY_REQUEST_COUNT("/actuator/request-count", HttpMethod.GET, true, "HZERO网关api请求计数信息"),
    GATEWAY_ROUTES_QUERY("/actuator/gateway/routes", HttpMethod.GET, true, "HZERO网关已生效路由信息查询"),
    GATEWAY_ROUTES_REFRESH("/actuator/gateway/refresh", HttpMethod.POST, true, "HZERO网关刷新路由"),
    /**
     * Actuator Endpoint
     */
    ACTUATOR_PERMISSION("/v2/actuator/permission", HttpMethod.GET, false, "获取服务权限信息");

    private String endpoint;

    private HttpMethod method;

    private boolean useManagementPort;

    private String description;

    StaticEndpoint(String endpoint, HttpMethod method, boolean useManagementPort, String description) {
        this.endpoint = endpoint;
        this.method = method;
        this.useManagementPort = useManagementPort;
        this.description = description;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public boolean isUseManagementPort() {
        return useManagementPort;
    }

    public String getDescription() {
        return description;
    }
}
