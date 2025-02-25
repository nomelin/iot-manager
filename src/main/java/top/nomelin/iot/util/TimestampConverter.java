package top.nomelin.iot.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class TimestampConverter {

    private static final List<DateTimeFormatter> DATE_FORMATTERS = new ArrayList<>();
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^\\d+$");// 匹配纯数字字符串

    static {
        // 初始化常用日期格式，带时区和UTC默认时区
        ZoneId utc = ZoneId.of("Asia/Shanghai");

        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(utc));
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS").withZone(utc));
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(utc));
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss").withZone(utc));
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-MM-dd-H:m:s").withZone(utc));
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-M-d-H:m:s").withZone(utc));
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy-M-d-H-m-s").withZone(utc));//前端导出文件的时间戳格式
        DATE_FORMATTERS.add(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").withZone(utc));
        DATE_FORMATTERS.add(DateTimeFormatter.ISO_INSTANT.withZone(utc));
        DATE_FORMATTERS.add(DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(utc));
        DATE_FORMATTERS.add(DateTimeFormatter.ISO_ZONED_DATE_TIME.withZone(utc));
        DATE_FORMATTERS.add(DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(utc));
        DATE_FORMATTERS.add(DateTimeFormatter.ISO_LOCAL_DATE.withZone(utc));
    }

    /**
     * 自动转换为毫秒级时间戳
     *
     * @param object 任意对象，支持Date、Calendar、TemporalAccessor、Number、String（数字以及各种日期格式）
     * @return 时间戳（毫秒级）
     */
    public static long convertToMillis(Object object) {
        if (object == null) {
            throw new IllegalArgumentException("时间对象不能为null");
        }

        if (object instanceof Date) {
            return ((Date) object).getTime();
        } else if (object instanceof Calendar) {
            return ((Calendar) object).getTimeInMillis();
        } else if (object instanceof TemporalAccessor) {
            return parseTemporalAccessor((TemporalAccessor) object);
        } else if (object instanceof Number) {
            return parseNumber(((Number) object).longValue());
        } else if (object instanceof String) {
            return parseString((String) object);
        } else {
            throw new IllegalArgumentException("不支持的时间类型: " + object.getClass().getName());
        }
    }

    private static long parseTemporalAccessor(TemporalAccessor temporal) {
        try {
            return Instant.from(temporal).toEpochMilli();
        } catch (DateTimeException e) {
            // 处理LocalDateTime等无时区类型
            if (temporal instanceof LocalDateTime) {
                return ((LocalDateTime) temporal).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            } else if (temporal instanceof LocalDate) {
                return ((LocalDate) temporal).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }
            throw new IllegalArgumentException("不支持的temporal时间类型: " + temporal.getClass().getName(), e);
        }
    }

    //如果数字很小，则无法区分，都会解析为秒级时间戳。
    private static long parseNumber(long value) {
        if (value < 1_000_000_000_000L) { // 秒级时间戳（约1970-2001年）
            return value * 1_000L;
        } else if (value < 1_000_000_000_000_000L) { // 毫秒级（2001-2286年）
            return value;
        } else if (value < 1_000_000_000_000_000_000L) { // 微秒级
            return value / 1_000L;
        } else { // 纳秒级
            return value / 1_000_000L;
        }
    }

    private static long parseString(String str) {
        // 尝试解析为数值
        if (NUMERIC_PATTERN.matcher(str).matches()) {
            try {
                return parseNumber(Long.parseLong(str));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("无法处理的数字类型时间: " + str, e);
            }
        }

        // 尝试各种日期格式
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                TemporalAccessor temporal = formatter.parse(str);
                return Instant.from(temporal).toEpochMilli();
            } catch (DateTimeException ignored) {
                // 继续尝试下一个格式
            }
        }

        throw new IllegalArgumentException("无法处理的字符串类型时间: " + str);
    }
}
