package top.nomelin.iot.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.SystemException;

public class JsonUtil {
    // 线程安全的 ObjectMapper (配置完成后不可变)
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())  // 支持 Java 8 时间类型
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)  // 允许空对象
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);  // 日期格式化为字符串

    private JsonUtil() {
    } // 防止实例化

    /**
     * 将任意对象转换为 JSON 字符串
     *
     * @param object 需要转换的对象
     * @return JSON 字符串
     * @throws RuntimeException 如果转换失败
     */
    public static String toJsonString(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new SystemException(CodeMessage.JSON_HANDEL_ERROR);
        }
    }

    /**
     * 美化输出的 JSON 字符串
     */
    public static String toPrettyJsonString(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new SystemException(CodeMessage.JSON_HANDEL_ERROR);
        }
    }
}
