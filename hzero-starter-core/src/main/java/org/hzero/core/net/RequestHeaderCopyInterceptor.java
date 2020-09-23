package org.hzero.core.net;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.MacSigner;
import org.springframework.security.jwt.crypto.sign.Signer;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.choerodon.core.convertor.ApplicationContextHelper;
import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.core.oauth.DetailsHelper;

import org.hzero.core.base.TokenConstants;
import org.hzero.core.properties.CoreProperties;

/**
 * RestTemplate自动复制请求头信息
 *
 * @author gaokuo.dai@hand-china.com 2018年9月4日下午7:05:06
 */
public class RequestHeaderCopyInterceptor implements ClientHttpRequestInterceptor {

    private static final String CACHE_CONTROL_NO_CACHE = "no-cache";

    private static final Logger logger = LoggerFactory.getLogger(RequestHeaderCopyInterceptor.class);

    private static final String OAUTH_TOKEN_PREFIX = TokenConstants.HEADER_BEARER + " ";
    private final Set<String> ignoreHeader = new CopyOnWriteArraySet<>();

    @Override
    @Nonnull
    public ClientHttpResponse intercept(@Nonnull HttpRequest request, @Nonnull byte[] body, @Nonnull ClientHttpRequestExecution execution) throws IOException {
        try {
            ServletRequestAttributes originRequestAttribute = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (originRequestAttribute != null) {
                HttpServletRequest originHttpServletRequest = originRequestAttribute.getRequest();
                Enumeration<String> originHeaderNames = originHttpServletRequest.getHeaderNames();
                HttpHeaders aimHttpHeaders = request.getHeaders();
                if (originHeaderNames != null) {
                    while (originHeaderNames.hasMoreElements()) {
                        String key = originHeaderNames.nextElement();
                        if (ignoreHeader.contains(key)) {
                            continue;
                        }
                        String value = originHttpServletRequest.getHeader(key);
                        aimHttpHeaders.add(key, value);
                    }
                }
                // 没有token的话，补充token，若没有登录信息，补充匿名用户
                if (!aimHttpHeaders.containsKey(TokenConstants.JWT_TOKEN)) {
                    ApplicationContext applicationContext = ApplicationContextHelper.getContext();
                    CoreProperties coreProperties = applicationContext.getBean(CoreProperties.class);
                    ObjectMapper objectMapper = applicationContext.getBean(ObjectMapper.class);
                    Signer signer = new MacSigner(coreProperties.getOauthJwtKey());
                    String token = null;
                    if (SecurityContextHolder.getContext() != null
                            && SecurityContextHolder.getContext().getAuthentication() != null
                            && SecurityContextHolder.getContext().getAuthentication().getDetails() instanceof OAuth2AuthenticationDetails) {
                        OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) SecurityContextHolder
                                .getContext().getAuthentication().getDetails();
                        if (details.getTokenType() != null && details.getTokenValue() != null) {
                            token = details.getTokenType() + " " + details.getTokenValue();
                        } else if (details.getDecodedDetails() instanceof CustomUserDetails) {
                            token = OAUTH_TOKEN_PREFIX
                                    + JwtHelper.encode(objectMapper.writeValueAsString(details.getDecodedDetails()), signer).getEncoded();
                        }
                    }
                    if (token == null) {
                        token = OAUTH_TOKEN_PREFIX + JwtHelper.encode(objectMapper.writeValueAsString(DetailsHelper.getAnonymousDetails()), signer).getEncoded();
                    }
                    aimHttpHeaders.add(TokenConstants.JWT_TOKEN, token);
                }
                aimHttpHeaders.add(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_NO_CACHE);
            }
        } catch (Exception e) {
            logger.warn("can not copy header info automatic", e.getCause());
        }
        return execution.execute(request, body);
    }

    public void addIgnoreHeader(String key) {
        ignoreHeader.add(key);
    }
}
