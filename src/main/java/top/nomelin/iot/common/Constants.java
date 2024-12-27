package top.nomelin.iot.common;

import org.apache.tsfile.file.metadata.enums.CompressionType;
import org.apache.tsfile.file.metadata.enums.TSEncoding;
import org.springframework.stereotype.Component;

/**
 * @author nomelin
 */
@Component
public class Constants {
    public static final String DEFAULT_PASSWORD = "123456";
    public static final String TOKEN = "token";
    public static final int TOKEN_EXPIRE_TIME = 24 * 7; // 7天

    public static final TSEncoding TS_ENCODING = TSEncoding.PLAIN;

    public static final CompressionType COMPRESSION_TYPE = CompressionType.UNCOMPRESSED;

    public static final String DATABASE_PREFIX = "root.data.user_";

    public static final String TEMPLATE_PREFIX = "template_";

    public static final String DEVICE_PREFIX = "device_";

}
