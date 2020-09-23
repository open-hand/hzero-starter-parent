package org.hzero.apollo.config.client;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import org.hzero.apollo.config.client.namespace.NamespaceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * 环境变量刷新类，用于刷新Spring上下文的配置项
 * @author XCXCXCXCX
 */
@Component
public class EnvironmentRefresher implements ConfigChangeListener, ApplicationContextAware, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentRefresher.class);

    private ApplicationContext applicationContext;

    private final ApolloConfigListenerProperties properties;

    public EnvironmentRefresher(ApolloConfigListenerProperties properties) {
        this.properties = properties;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for(String namespace : NamespaceManager.get()){
            Config config = ConfigService.getConfig(namespace);
            if(properties.getInterestedKeys() == null && properties.getInterestedKeyPrefixes() == null){
                config.addChangeListener(this);
            }else{
                config.addChangeListener(this, properties.getInterestedKeys(), properties.getInterestedKeyPrefixes());
            }
        }
    }

    @Override
    public void onChange(ConfigChangeEvent changeEvent) {
        refreshEnvironment(changeEvent);
    }

    private void refreshEnvironment(ConfigChangeEvent changeEvent) {
        LOGGER.info("Refreshing environment!");
        applicationContext.publishEvent(new EnvironmentChangeEvent(changeEvent.changedKeys()));
        LOGGER.info("Refreshed environment!");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
