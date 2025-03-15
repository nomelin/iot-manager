package top.nomelin.iot.service.buffer;

import top.nomelin.iot.common.annotation.LogExecutionTime;
import top.nomelin.iot.service.DataService;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class DeviceBuffer {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DeviceBuffer.class);
    private final int deviceId;
    private final DataService dataService;
    private final int bufferSize;
    private final long flushInterval;//刷新间隔，单位ms
    private final ReentrantLock lock = new ReentrantLock();//控制写入和刷新操作的锁
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final List<DataPoint> buffer = new ArrayList<>();
    private final Set<String> measurements = new LinkedHashSet<>();

    public DeviceBuffer(int deviceId, DataService dataService, int bufferSize, long flushInterval) {
        this.deviceId = deviceId;
        this.dataService = dataService;
        this.bufferSize = bufferSize;
        this.flushInterval = flushInterval;
        scheduleFlush();
    }

    private void scheduleFlush() {
        scheduler.scheduleAtFixedRate(this::flush, flushInterval, flushInterval, TimeUnit.MILLISECONDS);
    }

    public void addData(Map<String, Object> rawData) {
        lock.lock();
        try {
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

            //一个时间戳可能对应多个数据点
            //TODO 现在不能处理数据名称发生变化的情况
            for (Map<String, Object> point : dataPoints) {
                if (measurements.isEmpty()) {
                    measurements.addAll(point.keySet());
                }
                buffer.add(new DataPoint(timestamp, point));
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