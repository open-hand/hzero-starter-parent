package org.hzero.core.jackson.serializer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.hzero.core.jackson.JacksonConstant;
import org.hzero.core.jackson.annotation.IgnoreTimeZone;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;

import io.choerodon.core.oauth.CustomUserDetails;
import io.choerodon.core.oauth.DetailsHelper;

/**
 * <p>
 * 时间序列化，按照用户首选项设置的时区对时间进行转换
 * 如果属性被标记为忽略时区，则不做时区转换
 *
 * @author qingsheng.chen 2018/8/27 星期一 9:27
 * @see IgnoreTimeZone
 * </p>
 */
public class DateSerializer extends JsonSerializer<Date> implements ContextualSerializer {
    private boolean ignoreTimeZone = false;

    @Override
    public void serialize(Date date, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        SimpleDateFormat dateFormatGmt = new SimpleDateFormat(JacksonConstant.DEFAULT_DATE_FORMAT);
        if (ignoreTimeZone) {
            jsonGenerator.writeString(dateFormatGmt.format(date));
        } else {
            CustomUserDetails details = DetailsHelper.getUserDetails();
            if (details != null && details.getTimeZone() != null) {
                dateFormatGmt.setTimeZone(TimeZone.getTimeZone(details.getTimeZone()));
            }
            jsonGenerator.writeString(dateFormatGmt.format(date));
        }
    }


    private DateSerializer setIgnoreTimeZone(boolean ignoreTimeZone) {
        this.ignoreTimeZone = ignoreTimeZone;
        return this;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        return new DateSerializer().setIgnoreTimeZone(property != null && property.getMember().hasAnnotation(IgnoreTimeZone.class));
    }
}
