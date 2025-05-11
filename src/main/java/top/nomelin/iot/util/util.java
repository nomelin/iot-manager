package top.nomelin.iot.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.nomelin.iot.common.Constants;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
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
    public static Long alignToEast8Zone(Long timestamp, int granularity) {
        if (timestamp == null) {
            return null;
        }
        if (granularity == 1 || granularity == 0) {
            return timestamp;// 1ms或者不聚合，提前返回，以便提高性能
        }

        ZoneId zone = ZoneId.of("Asia/Shanghai");// 东八区
        Instant instant = Instant.ofEpochMilli(timestamp);
        ZonedDateTime zdt = instant.atZone(zone);

        switch (granularity) {
            case 1000 -> // 1s
                    zdt = zdt.withNano(0);
            case 5_000 -> // 5s
                    zdt = zdt.withNano(0).withSecond(zdt.getSecond() / 5 * 5);
            case 15_000 -> // 15s
                    zdt = zdt.withNano(0).withSecond(zdt.getSecond() / 15 * 15);
            case 30_000 -> // 30s
                    zdt = zdt.withNano(0).withSecond(zdt.getSecond() / 30 * 30);
            case 60_000 -> // 1min
                    zdt = zdt.withSecond(0).withNano(0);
            case 300_000 -> // 5min
                    zdt = zdt.withSecond(0).withNano(0).withMinute(zdt.getMinute() / 5 * 5);
            case 900_000 -> // 15min
                    zdt = zdt.withSecond(0).withNano(0).withMinute(zdt.getMinute() / 15 * 15);
            case 1_800_000 -> // 30min
                    zdt = zdt.withSecond(0).withNano(0).withMinute(zdt.getMinute() / 30 * 30);
            case 3_600_000 -> // 1h
                    zdt = zdt.withMinute(0).withSecond(0).withNano(0);
            case 7_200_000 -> // 2h
                    zdt = zdt.withMinute(0).withSecond(0).withNano(0).withHour(zdt.getHour() / 2 * 2);
            case 21_600_000 -> // 6h
                    zdt = zdt.withMinute(0).withSecond(0).withNano(0).withHour(zdt.getHour() / 6 * 6);
            case 43_200_000 -> // 12h
                    zdt = zdt.withMinute(0).withSecond(0).withNano(0).withHour(zdt.getHour() < 12 ? 0 : 12);
            case 86_400_000 -> // 1d
                    zdt = zdt.withHour(0).withMinute(0).withSecond(0).withNano(0);
            case 259_200_000 -> // 3d
                    zdt = zdt.withHour(0).withMinute(0).withSecond(0).withNano(0)
                            .withDayOfMonth(zdt.getDayOfMonth() - ((zdt.getDayOfMonth() - 1) % 3));
            case 604_800_000 -> // 1w
                    zdt = zdt.with(ChronoField.DAY_OF_WEEK, 1)
                            .withHour(0).withMinute(0).withSecond(0).withNano(0);
            case 1_209_600_000 -> {// 2w
                // 对齐到最近一个「本地时间周一 + 0点」，然后向前调整到偶数周开始
                ZonedDateTime weekStart = zdt.with(ChronoField.DAY_OF_WEEK, 1)
                        .withHour(0).withMinute(0).withSecond(0).withNano(0);
                long epochWeek = weekStart.toEpochSecond() / 604800; // 秒为单位的一周
                if (epochWeek % 2 != 0) {
                    weekStart = weekStart.minusWeeks(1);
                }
                zdt = weekStart;
            }
            default -> {
                log.error("不支持的查询粒度: {}", granularity);
                throw new IllegalArgumentException("不支持的查询粒度: " + granularity);
            }
        }
//        log.info("对齐到东八区时间窗口: {}->{},granularity: {}", timestamp, zdt.toInstant().toEpochMilli(), granularity);
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
