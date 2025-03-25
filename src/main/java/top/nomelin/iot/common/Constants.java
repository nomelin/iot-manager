package top.nomelin.iot.common;

import org.apache.tsfile.file.metadata.enums.CompressionType;
import org.apache.tsfile.file.metadata.enums.TSEncoding;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.SystemException;

import java.util.List;

/**
 * @author nomelin
 */
@Component
public class Constants {
    public static final Logger log = org.slf4j.LoggerFactory.getLogger(Constants.class);
    public static final String DEFAULT_PASSWORD = "123456";
    public static final String TOKEN = "token";
    public static final int TOKEN_EXPIRE_TIME = 24 * 7; // 7天
    //实际上数据库是root.data。加上user_是为了区分用户数据，只是普通路径
    public static final String DATABASE_PREFIX = "root.data.user_";
    public static final String TEMPLATE_PREFIX = "template_";
    public static final String DEVICE_PREFIX = "device_";
    public static final List<Integer> VALID_STORAGE_AGGREGATION_TIME = List.of(1, 10, 100, 1_000, 10_000, 100_000);
    //查询聚合时间粒度。0表示不聚合
    public static final List<Integer> VALID_QUERY_AGGREGATION_TIME = List.of(
            0,
            1, 1000, 5000, 15000, 30000,//1ms,1s,5s,15s,30s
            60_000, 300_000, 900_000, 1_800_000,//1min,5min,15min,30min
            3600_000, 86400000);//1h,1d
    public static final int DEFAULT_FILE_BATCH_SIZE = 500;
    public static final String DEFAULT_FILE_BATCH_SIZE_STR = "500";
    public static final int DEFAULT_MERGE_TIMESTAMP_NUM = -1;//默认全量合并
    public static final String DEFAULT_MERGE_TIMESTAMP_NUM_STR = "-1";//默认全量合并
    //标签的属性值名称
    public static final String TAG = "tag";
    //没有标签的属性值
    public static final String NO_TAG = "NO_TAG";
    public static final int SYSTEM_SENDER_ID = 0;
    public static final String TEMPLATE_CACHE_PREFIX = "template";
    public static final String DEVICE_CACHE_PREFIX = "device";
    public static final String ALL_ARGS_KEY = "#ALL_ARGS#";
    public static final int TASK_AUTO_CLEAN_INTERVAL = 1000 * 60 * 5; // 5分钟
    public static TSEncoding TS_ENCODING = TSEncoding.PLAIN;
    public static CompressionType COMPRESSION_TYPE = CompressionType.UNCOMPRESSED;//考虑将压缩配置作为每个设备特有的配置。

    @Value("${iotdb.storage.ts-encoding:PLAIN}")
    public void setTsEncoding(String tsEncoding) {
        switch (tsEncoding) {
            case "PLAIN" -> TS_ENCODING = TSEncoding.PLAIN;
            default -> throw new SystemException(CodeMessage.CONFIG_ERROR, "iotdb.storage.ts-encoding 配置错误。" +
                    "只有PLAIN编码是所有数据类型都支持的。");
        }
        log.info("iotdb.storage.ts-encoding 配置为 {}", TS_ENCODING);
    }
    @Value("${iotdb.storage.compression-type:UNCOMPRESSED}")
    public void setCompressionType(String compressionType) {
        switch (compressionType) {
            case "UNCOMPRESSED" -> COMPRESSION_TYPE = CompressionType.UNCOMPRESSED;
            case "SNAPPY" -> COMPRESSION_TYPE = CompressionType.SNAPPY;
            case "GZIP" -> COMPRESSION_TYPE = CompressionType.GZIP;
            case "LZ4" -> COMPRESSION_TYPE = CompressionType.LZ4;
            case "ZSTD" -> COMPRESSION_TYPE = CompressionType.ZSTD;
            case "LZMA2" -> COMPRESSION_TYPE = CompressionType.LZMA2;
            default -> throw new SystemException(CodeMessage.CONFIG_ERROR, "iotdb.storage.compression-type 配置错误");
        }
        log.info("iotdb.storage.compression-type 配置为 {}", COMPRESSION_TYPE);
    }


}
