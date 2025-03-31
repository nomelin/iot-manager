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
    public static final String REGISTER_MESSAGE_TITLE = "重要提示";
    public static final String REGISTER_MESSAGE_CONTENT =
            "<p>" +
                    "1.存储模式：<br>" +
                    "<b>COVER(覆盖)</b>模式: 对于时间戳相同的数据，新数据会<b>覆盖</b>旧数据，旧数据会<b>丢失</b>。此模式具有最好的写入性能，查询性能，" +
                    "空间性能，对于一般的物联网应用，建议使用此模式。如果某类设备存在时间戳相同的数据并且要求这些数据全部完整存储，则" +
                    "需要使用其它模式。<br>" +
                    "<b>COMPATIBLE(兼容)</b>模式: 支持任意时间戳相同的数据的并存。写入性能，查询性能，空间性能都较差。<br>" +
                    "<b>PERFORMANCE(性能)</b>模式: 支持秒级时间戳粒度的时间戳相同数据的并存，每秒最多并存1000条数据。写入性能较差，查询性能、空间性能较好。<br>" +
                    "</p>" +
                    "<p>" +
                    "2.查询聚合：如果服务器<b>带宽较低</b>，则当查询的时间范围较大时，数据量也较大，尽管已经采用了压缩策略，但仍然需要大量时间来网络传输数据。" +
                    "因此，如果无法在合适的时间内查询成功，则需要设置更大的查询聚合时间粒度。(可从F12控制台网络面板中查看延时情况)<br>" +
                    "</p>" +
                    "<p>" +
                    "3.个人中心可进入DEBUG页面：可以查看服务器的运行情况，包括MySQL和IoTDB的连接情况。请<b>谨慎操作</b>。" +
                    "</p>" +
                    "<p>" +
                    "4.告警设置：强烈建议在触发动作中配置一定的<b>静默时间</b>，以免短时间内大量触发告警推送" +
                    "</p>";
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
