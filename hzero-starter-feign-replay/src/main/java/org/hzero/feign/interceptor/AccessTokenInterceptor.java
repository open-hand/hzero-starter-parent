package org.hzero.feign.interceptor;

import feign.RequestTemplate;
import org.apache.commons.lang3.StringUtils;
import org.hzero.core.variable.RequestVariableHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author qingsheng.chen@hand-china.com
 */
public class AccessTokenInterceptor implements FeignRequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        if (RequestContextHolder.getRequestAttributes() != null) {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
                HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
                String authorization = request.getHeader(RequestVariableHolder.HEADER_AUTH);
                if (StringUtils.isNotBlank(authorization)) {
                    template.header(RequestVariableHolder.HEADER_AUTH, authorization);
                }
            }
        }
    }
}
