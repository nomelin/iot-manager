package top.nomelin.iot.util;

import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

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
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

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

    /**
     * 自动处理各种时间格式，将其转换为毫秒时间戳
     */
    public static long convertToMillis(Object timestamp) {
        if (timestamp instanceof Long) {
            // 如果是 long 类型，直接返回
            return (Long) timestamp;
        } else if (timestamp instanceof Date) {
            // 如果是 Date 类型，返回毫秒时间戳
            return ((Date) timestamp).getTime();
        } else if (timestamp instanceof String timeStr) {
            // 如果是 String 类型，尝试解析为日期时间
            try {
                // 先尝试 ISO 格式的日期时间
                SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                return isoFormat.parse(timeStr).getTime();
            } catch (ParseException e) {
                // 如果 ISO 格式失败，再尝试常见的日期格式
                SimpleDateFormat standardFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                try {
                    return standardFormat.parse(timeStr).getTime();
                } catch (ParseException ex) {
                    throw new BusinessException(CodeMessage.TIME_FORMAT_ERROR, "时间格式错误：timestamp=" + timestamp);
                }
            }
        } else {
            throw new BusinessException(CodeMessage.TIME_FORMAT_ERROR, "时间格式错误：timestamp=" + timestamp);
        }
    }

}
