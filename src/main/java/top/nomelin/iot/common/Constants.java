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

    //实际上数据库是root.data。加上user_是为了区分用户数据，只是普通路径
    public static final String DATABASE_PREFIX = "root.data.user_";

    public static final String TEMPLATE_PREFIX = "template_";

    public static final String DEVICE_PREFIX = "device_";

}
