package top.nomelin.iot.service.buffer;

import top.nomelin.iot.common.annotation.LogExecutionTime;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.SystemException;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.enums.IotDataType;
import top.nomelin.iot.service.DataService;
import top.nomelin.iot.service.DeviceService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static top.nomelin.iot.util.NumUtil.convertValue;

public class DeviceBuffer {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DeviceBuffer.class);
    private final int deviceId;
    private final Device device;
    private final DataService dataService;
    private final int bufferSize;
    private final long flushInterval;//刷新间隔，单位ms
    private final ReentrantLock lock = new ReentrantLock();//控制写入和刷新操作的锁
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final List<String> measurementList; // 来自设备配置的固定测量项
    // 双缓冲区结构
    private List<DataPoint> currentBuffer;
    private List<DataPoint> backBuffer;

    public DeviceBuffer(int deviceId, DataService dataService, DeviceService deviceService, int bufferSize, long flushInterval) {
        this.deviceId = deviceId;
        this.dataService = dataService;
        this.bufferSize = bufferSize;
        this.flushInterval = flushInterval;
        this.device = deviceService.getDeviceByIdWithoutCheck(deviceId);
        this.measurementList = new ArrayList<>(device.getConfig().getDataTypes().keySet());
        currentBuffer = new ArrayList<>(bufferSize);
        backBuffer = new ArrayList<>(bufferSize);
        scheduleFlush();
        log.info("创建设备缓冲区，设备ID: {}，数据缓冲区大小: {}，刷新间隔: {}ms", deviceId, bufferSize, flushInterval);
    }

    private void scheduleFlush() {
        scheduler.scheduleAtFixedRate(this::flush, flushInterval, flushInterval, TimeUnit.MILLISECONDS);
    }

    public void addData(Map<String, Object> rawData) {
        lock.lock();
        try {
            log.info("接收到设备ID: {}的数据，数据内容: {}", deviceId, rawData);
            parseData(rawData, currentBuffer);
            if (currentBuffer.size() >= bufferSize) {
                log.info("设备ID: {}，数据达到缓冲区大小{}，触发刷盘", deviceId, bufferSize);
                swapAndFlush();
            }
        } finally {
            lock.unlock();
        }
    }

    //解析数据，将原始数据转换为DataPoint对象，并放入buffer中
    private void parseData(Map<String, Object> rawData, List<DataPoint> targetBuffer) {
        for (Map.Entry<String, Object> entry : rawData.entrySet()) {
            long timestamp;
            try {
                timestamp = Long.parseLong(entry.getKey());
            } catch (NumberFormatException e) {
                log.error("设备ID: {}，解析时间戳失败: {}", deviceId, entry.getKey(), e);
                continue; // 跳过错误数据
            }

            List<Map<String, Object>> dataPoints = (List<Map<String, Object>>) entry.getValue();

            for (Map<String, Object> point : dataPoints) {
                Map<String, Object> convertedPoint = new HashMap<>();
                for (String measurement : measurementList) {
                    Object value = point.get(measurement);
                    if (value == null) {
                        log.error("设备ID: {}，数据缺少测量项: {}", deviceId, measurement);
                        continue; // 跳过错误数据
                    }
                    IotDataType dataType = device.getConfig().getDataTypes().get(measurement);
                    if (dataType == null) {
                        log.error("设备ID: {}，找不到测量项 '{}' 对应的数据类型", deviceId, measurement);
                        continue;
                    }
                    Object convertedValue = convertValue(value, dataType);
                    convertedPoint.put(measurement, convertedValue);
                }
                targetBuffer.add(new DataPoint(timestamp, convertedPoint));
            }
        }
    }

    private void flush() {
        swapAndFlush();
    }

    private void swapAndFlush() {
        List<DataPoint> toFlush;
        lock.lock();
        try {
            // 1. 交换缓冲区引用（耗时纳秒级）
            List<DataPoint> temp = currentBuffer;
            currentBuffer = backBuffer;
            backBuffer = temp;

            // 2. 获取待刷盘数据的引用（不复制数据）
            toFlush = backBuffer;

            // 3. 立即重置后台缓冲区（不影响已获取的toFlush引用）
            backBuffer = new ArrayList<>(bufferSize);
        } finally {
            lock.unlock();
        }

        if (!toFlush.isEmpty()) {
            scheduler.execute(() -> flushBuffer(toFlush));
        }
    }

    @LogExecutionTime
    private void flushBuffer(List<DataPoint> buffer) {
        try {
            List<Long> timestamps = new ArrayList<>();
            List<List<Object>> values = new ArrayList<>();

            for (DataPoint dp : buffer) {
                timestamps.add(dp.timestamp());
                List<Object> row = new ArrayList<>();
                for (String measurement : measurementList) {
                    row.add(dp.data().get(measurement));
                }
                values.add(row);
            }

            dataService.insertBatchRecord(
                    deviceId,
                    timestamps,
                    null, // tag
                    measurementList,
                    values,
                    -1    // mergeTimestampNum
            );
            log.info("刷新数据到设备ID: {}，共{}条数据", deviceId, buffer.size());
        } catch (Exception e) {
            log.error("刷新数据到设备ID: {}失败，原因: {}", deviceId, e.getMessage());
            throw new SystemException(CodeMessage.UPLOAD_DATA_FAILED,
                    "刷新数据到设备ID: " + deviceId + "失败，原因: " + e.getMessage(),
                    e);
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("设备ID: {}，调度器未能在30秒内终止，强制关闭", deviceId);
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("设备ID: {}，关闭调度器时被中断", deviceId, e);
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}