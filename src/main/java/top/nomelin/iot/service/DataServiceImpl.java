package top.nomelin.iot.service;

import org.apache.tsfile.enums.TSDataType;
import org.springframework.stereotype.Service;
import top.nomelin.iot.common.enums.IotDataType;
import top.nomelin.iot.dao.IoTDBDao;
import top.nomelin.iot.model.Config;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.DeviceTable;
import top.nomelin.iot.model.Record;
import top.nomelin.iot.service.aggregation.Aggregator;
import top.nomelin.iot.service.aggregation.InsertMode;
import top.nomelin.iot.service.aggregation.QueryMode;
import top.nomelin.iot.service.storage.StorageStrategy;
import top.nomelin.iot.service.storage.impl.CompatibleStorageStrategy;
import top.nomelin.iot.service.storage.impl.CoverStorageStrategy;
import top.nomelin.iot.service.storage.impl.PerformanceStorageStrategy;
import top.nomelin.iot.util.util;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DataServiceImpl implements DataService {
    private final DeviceService deviceService;
    private final IoTDBDao iotDBDao;
    private final Map<InsertMode, StorageStrategy> storageStrategies;

    public DataServiceImpl(DeviceService deviceService, IoTDBDao iotDBDao,
                           CoverStorageStrategy coverStrategy,
                           CompatibleStorageStrategy compatibleStrategy,
                           PerformanceStorageStrategy performanceStrategy) {
        this.deviceService = deviceService;
        this.iotDBDao = iotDBDao;
        this.storageStrategies = Map.of(
                InsertMode.COVER, coverStrategy,
                InsertMode.COMPATIBLE, compatibleStrategy,
                InsertMode.PERFORMANCE, performanceStrategy
        );
    }

    @Override
    public void insertBatchRecord(int deviceId, List<Long> timestamps,
                                  List<String> measurements, List<List<Object>> values) {
        Device device = deviceService.getDeviceById(deviceId);
        Config config = device.getConfig();

        // 转换数据类型
        List<List<TSDataType>> typesList = new ArrayList<>();
        for (int i = 0; i < timestamps.size(); i++) {
            List<TSDataType> types = measurements.stream()
                    .map(m -> IotDataType.convertToTsDataType(config.getDataTypes().get(m)))
                    .collect(Collectors.toList());
            typesList.add(types);
        }

        // 获取策略
        StorageStrategy strategy = storageStrategies.get(config.getInsertMode());
        String devicePath = util.getDevicePath(device.getUserId(), deviceId);

        strategy.storeData(devicePath, timestamps,
                Collections.nCopies(timestamps.size(), measurements),
                typesList, values, config.getAggregationTime());
    }

    @Override
    public DeviceTable queryRecord(int deviceId, long startTime, long endTime,
                                   List<String> selectMeasurements, int aggregationTime,
                                   QueryMode queryMode, List<List<Double>> thresholds) {
//        Device device = deviceService.getDeviceById(deviceId);
//        Config config = device.getConfig();
//        StorageStrategy strategy = storageStrategies.get(config.getInsertMode());
//        String devicePath = util.getDevicePath(device.getUserId(), deviceId);
//
//        // 对齐查询时间范围到东八区
//        long alignedStart = util.alignToEast8Zone(startTime, aggregationTime);
//        long alignedEnd = util.alignToEast8Zone(endTime, aggregationTime);
//
//        // 获取原始数据
//        DeviceTable rawTable = strategy.retrieveData(
//                devicePath, alignedStart, alignedEnd, selectMeasurements, aggregationTime);
//
//        //聚合到东八区时间窗口
//        DeviceTable resultTable = new DeviceTable();
//        Map<Long, Map<String, List<Object>>> aggregatedData = new TreeMap<>();
//
//        rawTable.getRecords().forEach((timestamp, record) -> {
//            // 对齐到东八区窗口
//            long window = util.alignToEast8Zone(timestamp, aggregationTime);
//
//            record.getFields().forEach((measurement, value) -> {
//                aggregatedData
//                        .computeIfAbsent(window, k -> new HashMap<>())
//                        .computeIfAbsent(measurement, k -> new ArrayList<>())
//                        .add(value);
//            });
//        });
//
//        // 生成聚合结果
//        aggregatedData.forEach((window, measurements) -> {
//            Record resultRecord = new Record();
//            measurements.forEach((measurement, values) -> {
//                Object aggregated = Aggregator.aggregate(values, queryMode);
//                resultRecord.getFields().put(measurement, aggregated);
//            });
//            resultTable.getRecords().put(window, resultRecord);
//        });
//
//        // 应用阈值过滤
//        if (thresholds != null) {
//            resultTable.getRecords().entrySet().removeIf(entry ->
//                    !passThreshold(entry.getValue(), selectMeasurements, thresholds));
//        }
//
//        return resultTable;
        return null;
    }

    private boolean passThreshold(Record record, List<String> measurements,
                                  List<List<Double>> thresholds) {
        for (int i = 0; i < measurements.size(); i++) {
            String measurement = measurements.get(i);
            List<Double> threshold = thresholds.get(i);
            if (threshold == null) {
                continue;
            }

            Object value = record.getFields().get(measurement);
            if (value == null) {
                return false;
            }

            double num = ((Number) value).doubleValue();
            if (threshold.get(0) != null && num < threshold.get(0)) {
                return false;
            }
            if (threshold.get(1) != null && num > threshold.get(1)) {
                return false;
            }
        }
        return true;
    }
}
