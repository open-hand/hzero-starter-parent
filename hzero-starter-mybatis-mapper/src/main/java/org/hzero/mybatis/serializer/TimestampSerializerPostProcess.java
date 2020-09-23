package org.hzero.mybatis.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import oracle.sql.TIMESTAMP;
import org.hzero.core.jackson.serializer.DateSerializer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.Date;

/**
 * @author qingsheng.chen@hand-china.com
 */
public class TimestampSerializerPostProcess implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (ObjectMapper.class.equals(bean.getClass())) {
            JavaTimeModule javaTimeModule = new JavaTimeModule();
            javaTimeModule.addSerializer(TIMESTAMP.class, new TimestampSerializer());

            ObjectMapper mapper = (ObjectMapper) bean;
            mapper.registerModules(javaTimeModule);
        }
        return bean;
    }
}
