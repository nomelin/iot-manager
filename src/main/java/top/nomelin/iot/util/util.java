package top.nomelin.iot.util;

import top.nomelin.iot.common.Constants;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * util
 *
 * @author nomelin
 * @since 2025/01/27 22:58
 **/
public class util {
    public static String getDevicePath(int userId, int deviceId) {
        return Constants.DATABASE_PREFIX + userId + Constants.DEVICE_PREFIX + deviceId;
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
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        Instant instant = Instant.ofEpochMilli(timestamp);
        ZonedDateTime zdt = instant.atZone(zone);

        switch (granularity) {
            case 1: // 1ms
                return timestamp;
            case 1000: // 1s
                zdt = zdt.withNano(0);
                break;
            case 60_000: // 1min
                zdt = zdt.withSecond(0).withNano(0);
                break;
            case 3_600_000: // 1h
                zdt = zdt.withMinute(0).withSecond(0).withNano(0);
                break;
            case 86_400_000: // 1day
                zdt = zdt.withHour(0).withMinute(0).withSecond(0).withNano(0);
                break;
            default:
                throw new IllegalArgumentException("不支持的查询粒度: " + granularity);
        }
        return zdt.toInstant().toEpochMilli();
    }

    // 对齐到存储时间窗口（向下取整）
    public static long alignToStorageWindow(long timestamp, int granularity) {
        return timestamp / granularity * granularity;
    }
}
