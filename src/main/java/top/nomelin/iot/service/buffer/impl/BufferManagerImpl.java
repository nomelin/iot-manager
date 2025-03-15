package top.nomelin.iot.service.buffer.impl;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import top.nomelin.iot.service.DataService;
import top.nomelin.iot.service.DeviceService;
import top.nomelin.iot.service.buffer.BufferManager;
import top.nomelin.iot.service.buffer.DeviceBuffer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BufferManagerImpl
 *
 * @author nomelin
 * @since 2025/03/15 15:32
 **/
@Service
public class BufferManagerImpl implements BufferManager {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(BufferManagerImpl.class);
    private final DataService dataService;
    private final DeviceService deviceService;
    private final ConcurrentHashMap<Integer, DeviceBuffer> buffers = new ConcurrentHashMap<>();
    private final int defaultBufferSize = 100; // TODO: 从配置获取
    private final long defaultFlushInterval = 5000; // TODO: 从配置获取

    public BufferManagerImpl(DataService dataService, DeviceService deviceService) {
        this.dataService = dataService;
        this.deviceService = deviceService;
    }

    @Override
    public void addData(int deviceId, Map<String, Object> data) {
        // 缓冲区不存在则创建,随后添加数据
        buffers.computeIfAbsent(deviceId, id ->
                new DeviceBuffer(id, dataService, deviceService, defaultBufferSize, defaultFlushInterval)
        ).addData(data);
    }

    // 清理不再使用的缓冲区
    @Override
    public void cleanupBuffer(int deviceId) {
        DeviceBuffer buffer = buffers.remove(deviceId);
        if (buffer != null) {
            buffer.shutdown();
        }
        log.info("Buffer of device {} has been cleaned up.", deviceId);
    }
}
