package top.nomelin.iot.service.buffer;

import top.nomelin.iot.common.annotation.LogExecutionTime;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.SystemException;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.enums.IotDataType;
import top.nomelin.iot.service.DataService;
import top.nomelin.iot.service.DeviceService;

import java.util.*;
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
    private final DeviceService deviceService;
    private final int bufferSize;
    private final long flushInterval;//刷新间隔，单位ms
    private final ReentrantLock lock = new ReentrantLock();//控制写入和刷新操作的锁
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final List<DataPoint> buffer = new ArrayList<>();
    private final Set<String> measurements = new LinkedHashSet<>();

    public DeviceBuffer(int deviceId, DataService dataService, DeviceService deviceService, int bufferSize, long flushInterval) {
        this.deviceId = deviceId;
        this.dataService = dataService;
        this.deviceService = deviceService;
        this.bufferSize = bufferSize;
        this.flushInterval = flushInterval;
        this.device = deviceService.getDeviceByIdWithoutCheck(deviceId);
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
            parseData(rawData);
            if (buffer.size() >= bufferSize) {
                log.info("设备ID: {}，数据达到缓冲区大小{}，开始刷新数据", deviceId, bufferSize);
                flush();
            }
        } finally {
            lock.unlock();
        }
    }


    //解析数据，将原始数据转换为DataPoint对象，并放入buffer中
    private void parseData(Map<String, Object> rawData) {
        for (Map.Entry<String, Object> entry : rawData.entrySet()) {
            long timestamp = Long.parseLong(entry.getKey());
            List<Map<String, Object>> dataPoints = (List<Map<String, Object>>) entry.getValue();

            for (Map<String, Object> point : dataPoints) {
                Map<String, Object> convertedPoint = new HashMap<>();
                for (Map.Entry<String, Object> dataEntry : point.entrySet()) {
                    String measurement = dataEntry.getKey();
                    Object value = dataEntry.getValue();

                    // 获取对应的数据类型
                    IotDataType dataType = device.getConfig().getDataTypes().get(measurement);
                    if (dataType == null) {
                        throw new SystemException(CodeMessage.UPLOAD_DATA_FAILED,
                                "设备配置错误，未找到测量项 '" + measurement + "' 的数据类型");
                    }

                    // 转换值
                    Object convertedValue = convertValue(value, dataType);
                    convertedPoint.put(measurement, convertedValue);
                }

                if (measurements.isEmpty()) {
                    measurements.addAll(convertedPoint.keySet());
                }
                buffer.add(new DataPoint(timestamp, convertedPoint));
            }
        }
    }

    @LogExecutionTime
    private void flush() {
        lock.lock();
        try {
            if (buffer.isEmpty()) {
                return;
            }

            List<Long> timestamps = new ArrayList<>();
            List<List<Object>> values = new ArrayList<>();
            List<String> measurementList = new ArrayList<>(measurements);

            for (DataPoint dp : buffer) {
                timestamps.add(dp.timestamp);
                List<Object> row = new ArrayList<>();
                for (String measurement : measurementList) {
                    row.add(dp.data.get(measurement));
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
            buffer.clear();
        } catch (Exception e) {
            log.error("刷新数据到设备ID: {}失败，原因: {}", deviceId, e.getMessage());
            throw new SystemException(CodeMessage.UPLOAD_DATA_FAILED,
                    "刷新数据到设备ID: " + deviceId + "失败，原因: " + e.getMessage(),
                    e);
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    private record DataPoint(long timestamp, Map<String, Object> data) {
    }
}