package top.nomelin.iot.service.buffer;

import java.util.Map;

/**
 * BufferManager
 *
 * @author nomelin
 * @since 2025/03/15 15:31
 **/
public interface BufferManager {
    /**
     * 添加数据到缓冲区。缓冲区会自行定时以及定量刷新批次数据到数据库。
     *
     * @param deviceId 设备ID
     * @param data     数据。格式为Map<String, Object>，其中key为时间戳，一个时间戳对应多个数据项，
     *                 Object为List<Map<String, Object>>,
     *                 其中Map<String, Object>的key为数据项名称，value为数据项值。
     */
    void addData(int deviceId, Map<String, Object> data);

    /**
     * 清理不再使用的缓冲区
     *
     * @param deviceId 设备ID
     */
    void cleanupBuffer(int deviceId);
}
