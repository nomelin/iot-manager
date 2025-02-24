package top.nomelin.iot.service.impl;

import cn.hutool.core.util.ObjectUtil;
import org.apache.tsfile.enums.TSDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.nomelin.iot.common.annotation.LogExecutionTime;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.common.exception.SystemException;
import top.nomelin.iot.model.Config;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.dto.DeviceTable;
import top.nomelin.iot.model.dto.Record;
import top.nomelin.iot.model.enums.IotDataType;
import top.nomelin.iot.model.enums.QueryAggregateFunc;
import top.nomelin.iot.service.DataService;
import top.nomelin.iot.service.DeviceService;
import top.nomelin.iot.service.storage.StorageStrategy;
import top.nomelin.iot.service.storage.StorageStrategyManager;
import top.nomelin.iot.util.util;

import java.util.*;
import java.util.stream.Collectors;

import static top.nomelin.iot.util.TimeUtil.isValidQueryAggregationTime;

@Service
public class DataServiceImpl implements DataService {
    private static final Logger log = LoggerFactory.getLogger(DataServiceImpl.class);
    private final DeviceService deviceService;

    private final StorageStrategyManager storageStrategyManager;

    public DataServiceImpl(DeviceService deviceService, StorageStrategyManager storageStrategyManager) {
        this.deviceService = deviceService;
        this.storageStrategyManager = storageStrategyManager;
    }

    @LogExecutionTime
    @Override
    public void insertBatchRecord(int deviceId, List<Long> timestamps,
                                  List<String> measurements, List<List<Object>> values, int mergeTimestampNum) {
        Device device = deviceService.getDeviceById(deviceId);
        Config config = device.getConfig();
        StorageStrategy strategy = storageStrategyManager.getStrategy(config.getStorageMode());// 获取存储策略
        String devicePath = util.getDevicePath(device.getUserId(), deviceId);

        batchInsert(timestamps, measurements, values, config, strategy, devicePath, mergeTimestampNum);
    }

    @LogExecutionTime
    @Override
    public void insertBatchRecord(Device device, List<Long> timestamps,
                                  List<String> measurements, List<List<Object>> values, int mergeTimestampNum) {
        Config config = device.getConfig();
        StorageStrategy strategy = storageStrategyManager.getStrategy(config.getStorageMode());// 获取存储策略
        String devicePath = util.getDevicePath(device.getUserId(), device.getId());

        batchInsert(timestamps, measurements, values, config, strategy, devicePath, mergeTimestampNum);
    }

    private void batchInsert(List<Long> timestamps, List<String> measurements,
                             List<List<Object>> values, Config config, StorageStrategy strategy, String devicePath,
                             int mergeTimestampNum) {
        // 把config中的物理量的类型转换为TSDataType
        List<TSDataType> types = measurements.stream()
                .map(m -> IotDataType.convertToTsDataType(config.getDataTypes().get(m))).toList();

        strategy.storeData(
                devicePath,
                timestamps,
                Collections.nCopies(timestamps.size(), measurements),//nCopies返回的是不可变集合。
                Collections.nCopies(timestamps.size(), types),
                values,
                config.getAggregationTime(),
                mergeTimestampNum
        );

        log.info("insertBatchRecord: devicePath={}, timestamps={}, measurements={}, types={}, values={}",
                devicePath, timestamps, measurements, types, values);
    }

    @LogExecutionTime(logArgs = true)
    @Override
    public DeviceTable queryRecord(int deviceId, long startTime, long endTime,
                                   List<String> selectMeasurements, Integer aggregationTime,
                                   QueryAggregateFunc queryAggregateFunc, List<List<Double>> thresholds) {
        Device device = deviceService.getDeviceById(deviceId);
        aggregationTime = checkAggregationTime(device, aggregationTime);
        StorageStrategy strategy = storageStrategyManager.getStrategy(device.getConfig().getStorageMode());
        String devicePath = util.getDevicePath(device.getUserId(), deviceId);
        return query(device, startTime, endTime, selectMeasurements, aggregationTime, queryAggregateFunc, thresholds, strategy, devicePath);
    }

    @LogExecutionTime(logArgs = true)
    @Override
    public DeviceTable queryRecord(Device device, long startTime, long endTime, List<String> selectMeasurements, Integer aggregationTime, QueryAggregateFunc queryAggregateFunc, List<List<Double>> thresholds) {
        aggregationTime = checkAggregationTime(device, aggregationTime);
        StorageStrategy strategy = storageStrategyManager.getStrategy(device.getConfig().getStorageMode());
        String devicePath = util.getDevicePath(device.getUserId(), device.getId());
        return query(device, startTime, endTime, selectMeasurements, aggregationTime, queryAggregateFunc, thresholds, strategy, devicePath);
    }

    private DeviceTable query(Device device, long startTime, long endTime, List<String> selectMeasurements, Integer aggregationTime, QueryAggregateFunc queryAggregateFunc, List<List<Double>> thresholds, StorageStrategy strategy, String devicePath) {
        if (ObjectUtil.isEmpty(selectMeasurements)) {
            selectMeasurements = device.getConfig().getMeasurements();
            log.info("queryRecord, selectMeasurements为空，使用设备的配置的所有物理量: {}", selectMeasurements);
        }

        // 对齐时间窗口
        long[] alignedTimeRange = alignTimeRange(startTime, endTime, aggregationTime);
        log.info("queryRecord 原始时间范围:{}-{}, 对齐后:{}-{}", startTime, endTime, alignedTimeRange[0], alignedTimeRange[1]);

        // 获取原始数据
        DeviceTable rawTable = strategy.retrieveData(
                devicePath, alignedTimeRange[0], alignedTimeRange[1], selectMeasurements, aggregationTime
        );

        // 阈值过滤（COUNT模式不处理）
        if (shouldApplyThreshold(queryAggregateFunc, thresholds)) {
            log.info("queryRecord 应用阈值过滤, selectMeasurements={}, thresholds={}", selectMeasurements, thresholds);
            applyThresholdFilter(rawTable, selectMeasurements, thresholds);
        } else {
            log.info("queryRecord 不应用阈值过滤, selectMeasurements={}, thresholds={}", selectMeasurements, thresholds);
        }

        DeviceTable aggregatedTable;
        // 聚合处理
        if (aggregationTime == 0 || ObjectUtil.isNull(queryAggregateFunc)) {
            log.info("queryRecord 不聚合, selectMeasurements={}, aggregationTime={}, QueryAggregateFunc={}",
                    selectMeasurements, aggregationTime, queryAggregateFunc);
            aggregatedTable = rawTable;//不聚合直接返回原始数据
        } else {
            log.info("queryRecord 应用聚合, selectMeasurements={}, aggregationTime={}, QueryAggregateFunc={}",
                    selectMeasurements, aggregationTime, queryAggregateFunc);
            aggregatedTable = aggregateRawData(rawTable, aggregationTime, queryAggregateFunc);
        }

        aggregatedTable.setDevicePath(devicePath);
        List<String> types = selectMeasurements.stream()
                .map(m -> device.getConfig().getDataTypes().get(m).toString()).toList();
        aggregatedTable.setTypes(types);

        log.info("queryRecord: device={}, startTime={}, endTime={}, selectMeasurements={}, aggregationTime={}, " +
                        "QueryAggregateFunc={}, thresholds={}",
                device, startTime, endTime, selectMeasurements, aggregationTime,
                queryAggregateFunc, thresholds);
        log.info("共查询到{}条时间戳（每个时间戳可能有多条记录，这里不统计）", aggregatedTable.getRecords().size());

        return aggregatedTable;
    }

    private int checkAggregationTime(Device device, Integer aggregationTime) {
        if (aggregationTime == null || aggregationTime < 0) {
            aggregationTime = 0;
        }
        if (aggregationTime > 0 && aggregationTime < device.getConfig().getAggregationTime()) {
            throw new BusinessException(CodeMessage.QUERY_AGGREGATION_TIME_ERROR,
                    "查询聚合粒度" + aggregationTime + "小于设备配置的最小聚合粒度" + device.getConfig().getAggregationTime());
        }
        if (!isValidQueryAggregationTime(aggregationTime)) {
            throw new BusinessException(CodeMessage.INVALID_QUERY_AGGREGATION_TIME_ERROR,
                    "查询聚合粒度" + aggregationTime + "不是有效的聚合粒度");
        }
        return aggregationTime;
    }

    private long[] alignTimeRange(long start, long end, int aggregationTime) {
        return new long[]{
                util.alignToEast8Zone(start, aggregationTime),
                util.alignToEast8Zone(end, aggregationTime)
        };
    }

    //按窗口聚合原始数据
    private DeviceTable aggregateRawData(DeviceTable rawTable, int aggregationTime, QueryAggregateFunc mode) {
        Map<Long, List<Record>> windowRecords = new TreeMap<>();

        // 按查询的时间窗口分组原始记录
        rawTable.getRecords().forEach((timestamp, records) ->
                records.forEach(record -> {
                    long window = util.alignToEast8Zone(timestamp, aggregationTime);
                    windowRecords.computeIfAbsent(window, k -> new ArrayList<>()).add(record);
                })
        );

        // 创建聚合结果表
        DeviceTable result = new DeviceTable();
        windowRecords.forEach((window, records) ->
                result.addRecord(window, createAggregatedRecord(records, mode))
        );

        return result;
    }

    //按聚合模式，将一组Record聚合成一个Record
    private Record createAggregatedRecord(List<Record> records, QueryAggregateFunc mode) {
        Record aggregated = new Record();

        // 收集所有测量值的映射
        Map<String, List<Object>> measurementValues = new HashMap<>();
        records.forEach(record ->
                record.getFields().forEach((measurement, value) ->
                        measurementValues.computeIfAbsent(measurement, k -> new ArrayList<>()).add(value)
                )
        );

        // 执行聚合操作
        measurementValues.forEach((measurement, values) ->
                aggregated.addField(measurement, performAggregation(values, mode))
        );

        return aggregated;
    }

    //对一个属性对应的一组值应用聚合操作
    private Object performAggregation(List<Object> values, QueryAggregateFunc mode) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        // 过滤掉null值, 并转换为double值
        List<Double> numericValues = values.stream()
                .filter(Objects::nonNull)
                .map(v -> ((Number) v).doubleValue()).toList();

        if (numericValues.isEmpty()) {
            return null;
        }

        return switch (mode) {
            case AVG -> numericValues.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            case MAX -> numericValues.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            case MIN -> numericValues.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            case SUM -> numericValues.stream().mapToDouble(Double::doubleValue).sum();
            case COUNT -> values.size();
            case FIRST -> values.get(0);
            case LAST -> values.get(values.size() - 1);
            default -> {
                log.error("Illegal aggregation mode: {}", mode);
                throw new SystemException(CodeMessage.ILLEGAL_AGGREGATION_ERROR, "Illegal aggregation mode: " + mode);
            }
        };
    }

    private boolean shouldApplyThreshold(QueryAggregateFunc mode, List<List<Double>> thresholds) {
        return thresholds != null && mode != QueryAggregateFunc.COUNT;
    }

    // 应用阈值过滤, 过滤掉不满足阈值条件的记录, 并清除空的时间窗口
    private void applyThresholdFilter(DeviceTable table, List<String> measurements, List<List<Double>> thresholds) {
        table.getRecords().replaceAll((timestamp, records) ->
                records.stream()
                        .filter(record -> meetsThresholdCriteria(record, measurements, thresholds))
                        .collect(Collectors.toList())
        );
        // 清除空的时间窗口
        table.purgeEmptyTimestamps();
    }

    //和applyThresholdFilter一样，只是使用并行流处理提升性能
    //TODO 配置化开关,或者根据规模自动选择
    private void applyThresholdFilterParallel(DeviceTable table, List<String> measurements, List<List<Double>> thresholds) {
        // 并行流处理提升性能
        Map<Long, List<Record>> filtered = table.getRecords().entrySet().parallelStream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .filter(record -> meetsThresholdCriteria(record, measurements, thresholds))
                                .collect(Collectors.toList())
                ));
        table.setRecords(filtered);
        table.purgeEmptyTimestamps();
    }

    // 判断一个Record是否满足阈值条件
    private boolean meetsThresholdCriteria(Record record, List<String> selectMeasurements, List<List<Double>> thresholds) {
        List<String> targetMeasurements = Optional.ofNullable(selectMeasurements)
                .orElseGet(() -> new ArrayList<>(record.getFields().keySet()));

        for (int i = 0; i < targetMeasurements.size(); i++) {
            String measurement = targetMeasurements.get(i);
            List<Double> threshold = thresholds.get(i);
            if (threshold == null) {
                continue;//null阈值表示不过滤
            }

            Object value = record.getFields().get(measurement);
            if (!isValueValid(value, threshold)) {
                return false;//一个Record有一个值不满足阈值条件，就会被过滤掉
            }
        }
        return true;
    }

    //判断一个值是否满足阈值条件
    private boolean isValueValid(Object value, List<Double> threshold) {
        if (!(value instanceof Number)) {
            return false;
        }

        double numericValue = ((Number) value).doubleValue();
        Double min = threshold.get(0);
        Double max = threshold.get(1);
        //左闭右闭
        return (min == null || numericValue >= min) &&
                (max == null || numericValue <= max);
    }
}
