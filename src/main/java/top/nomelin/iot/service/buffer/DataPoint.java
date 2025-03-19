package top.nomelin.iot.service.buffer;

import java.util.Map;

/**
 * 一行数据点。
 *
 * @author nomelin
 * @since 2025/03/19 20:39
 **/
public record DataPoint(long timestamp, Map<String, Object> data) {
}
