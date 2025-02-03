package top.nomelin.iot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.nomelin.iot.common.Constants;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * util
 *
 * @author nomelin
 * @since 2025/01/27 22:58
 **/
public class util {
    private static final Logger log = LoggerFactory.getLogger(util.class);

    public static String getDevicePath(int userId, int deviceId) {
        return Constants.DATABASE_PREFIX + userId + "." + Constants.DEVICE_PREFIX + deviceId;
    }

    // 存储粒度处理（向下取10的幂次）
    public static int adjustStorageGranularity(int milliseconds) {
        if (milliseconds <= 1) {
            return 1;
        }
        int exponent = (int) Math.floor(Math.log10(milliseconds));
        return (int) Math.pow(10, exponent);
    }

    // 对齐到东八区时间窗口
    public static long alignToEast8Zone(long timestamp, int granularity) {
        if (granularity == 1 || granularity == 0) {
            return timestamp;// 1ms或者不聚合，提前返回，以便提高性能
        }

        ZoneId zone = ZoneId.of("Asia/Shanghai");// 东八区
        Instant instant = Instant.ofEpochMilli(timestamp);
        ZonedDateTime zdt = instant.atZone(zone);

        switch (granularity) {
            case 1000 -> // 1s
                    zdt = zdt.withNano(0);//毫秒是纳秒（0-999,999,999）的前三位
            case 60_000 -> // 1min
                    zdt = zdt.withSecond(0).withNano(0);
            case 3_600_000 -> // 1h
                    zdt = zdt.withMinute(0).withSecond(0).withNano(0);
            case 86_400_000 -> // 1day
                    zdt = zdt.withHour(0).withMinute(0).withSecond(0).withNano(0);
            default -> {
                log.error("不支持的查询粒度: {}", granularity);
                throw new IllegalArgumentException("不支持的查询粒度: " + granularity);
            }
        }
        log.info("对齐到东八区时间窗口: {}->{},granularity: {}", timestamp, zdt.toInstant().toEpochMilli(), granularity);
        return zdt.toInstant().toEpochMilli();
    }

    // 对齐到存储时间窗口（向下取整）
    public static long alignToStorageWindow(long timestamp, int granularity) {
        if (granularity == 1) {
            return timestamp;
        }
        return timestamp / granularity * granularity;
    }

    public static List<Long> alignTimestamps(List<Long> timestamps, int storageGranularity) {
        if (storageGranularity == 1) {
            return timestamps;
        }
        return timestamps.stream()
                .map(timestamp -> alignToStorageWindow(timestamp, storageGranularity))
                .toList();
    }
}
