package top.nomelin.iot.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * TimeUtil
 *
 * @author nomelin
 * @since 2024/12/15 15:59
 **/
public class TimeUtil {

    /**
     * 将毫秒时间戳转换为日期字符串
     * 时区使用系统默认时区
     *
     * @param timestamp 毫秒时间戳
     * @return 格式化后的日期字符串
     */
    public static String timestampToDateString(long timestamp) {
        // 将时间戳转换为 LocalDateTime
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());

        // 定义日期格式化模式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 格式化并返回日期字符串
        return dateTime.format(formatter);
    }

    /**
     * 将日期字符串转换为毫秒时间戳
     * 支持多种格式，如"yyyy-MM-dd HH:mm:ss"、"yyyy/MM/dd HH:mm:ss"等
     * 时区使用系统默认时区
     *
     * @param dateString 日期字符串
     * @param format     日期格式
     * @return 毫秒时间戳
     * @throws IllegalArgumentException 如果格式不匹配
     */
    public static long dateStringToTimestamp(String dateString, String format) throws IllegalArgumentException {

        // 创建日期格式化器
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

        // 将日期字符串解析为 LocalDateTime
        LocalDateTime localDateTime = LocalDateTime.parse(dateString, formatter);

        // 将 LocalDateTime 转换为时间戳（毫秒）
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

    }

}