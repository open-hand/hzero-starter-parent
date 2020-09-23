package io.choerodon.resource.permission;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.swagger.common.SwaggerPluginSupport;

import io.choerodon.swagger.annotation.Permission;

/**
 * @author flyleft
 * 2018/4/16
 */
@Order(SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER + 1100)
public class PublicPermissionOperationPlugin implements OperationBuilderPlugin {

    private final Set<PublicPermission> publicPermissions = new HashSet<>();

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @Override
    public void apply(OperationContext context) {
        Permission permission = context.findAnnotation(Permission.class).orNull();
        if (permission != null && (permission.permissionPublic() || permission.permissionSign())) {
            publicPermissions.add(new PublicPermission(contextPath + context.requestMappingPattern(), context.httpMethod()));
        }

    }

    @Override
    public boolean supports(DocumentationType documentationType) {
        return true;
    }

    public Set<PublicPermission> getPublicPaths() {
        return publicPermissions;
    }
}
