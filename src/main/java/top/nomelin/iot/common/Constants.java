package top.nomelin.iot.common;

import org.apache.tsfile.file.metadata.enums.CompressionType;
import org.apache.tsfile.file.metadata.enums.TSEncoding;
import org.springframework.stereotype.Component;

import java.util.List;

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

    public static final List<Integer> VALID_STORAGE_AGGREGATION_TIME = List.of(1, 10, 100, 1_000, 10_000, 100_000);

    //查询聚合时间粒度。0表示不聚合
    public static final List<Integer> VALID_QUERY_AGGREGATION_TIME = List.of(0, 1, 1000, 60_000, 3600_000, 86400000);

}
