package top.nomelin.iot.service.impl;

import cn.hutool.core.util.ObjectUtil;
import org.apache.tsfile.enums.TSDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.nomelin.iot.common.Constants;
import top.nomelin.iot.common.GlobalConfig;
import top.nomelin.iot.common.annotation.CacheOp;
import top.nomelin.iot.common.annotation.LogExecutionTime;
import top.nomelin.iot.common.enums.CacheOpType;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.common.exception.SystemException;
import top.nomelin.iot.dao.IoTDBDao;
import top.nomelin.iot.model.Config;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.dto.DeviceTable;
import top.nomelin.iot.model.dto.Record;
import top.nomelin.iot.model.enums.IotDataType;
import top.nomelin.iot.model.enums.MessageType;
import top.nomelin.iot.model.enums.QueryAggregateFunc;
import top.nomelin.iot.service.DataService;
import top.nomelin.iot.service.DeviceService;
import top.nomelin.iot.service.MessageService;
import top.nomelin.iot.service.storage.ExtendFastAggregateQuery;
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
    private final IoTDBDao iotDBDao;

    private final StorageStrategyManager storageStrategyManager;

    private final MessageService messageService;

    private GlobalConfig globalConfig;

    public DataServiceImpl(DeviceService deviceService, IoTDBDao iotDBDao, StorageStrategyManager storageStrategyManager, MessageService messageService, GlobalConfig globalConfig) {
        this.deviceService = deviceService;
        this.iotDBDao = iotDBDao;
        this.storageStrategyManager = storageStrategyManager;
        this.messageService = messageService;
        this.globalConfig = globalConfig;
    }

    @Override
    public void insertBatchRecordAutoFormat(int deviceId, List<Long> timestamps, String tag,
                                            List<String> measurements, List<List<Object>> values, int mergeTimestampNum) {
        tag = validateTagForInsert(tag);
        List<String> measurementsCopy = addTagInMeasurementsAndValues(tag, measurements, values);
        Device device = deviceService.addDataTagsWithoutCheck(deviceId, Collections.singleton(tag));
        Config config = device.getConfig();
        StorageStrategy strategy = storageStrategyManager.getStrategy(config.getStorageMode());// 获取存储策略
        String devicePath = util.getDevicePath(device.getUserId(), deviceId);

        // 获取每个测量项的数据类型配置
        List<IotDataType> dataTypes = new ArrayList<>();
        for (String measurement : measurements) {
            IotDataType dataType = device.getConfig().getDataTypes().get(measurement);
            if (dataType == null) {
                throw new SystemException(CodeMessage.PARAM_LOST_ERROR,
                        "未找到测量项 '" + measurement + "' 的数据类型配置");
            }
            dataTypes.add(dataType);
        }

        for (int i = 0; i < timestamps.size(); i++) {
            List<Object> valueList = values.get(i);
            //-1是因为最后一列是前面添加的TAG，不需要处理类型
            for (int j = 0; j < valueList.size() - 1; j++) {
                Object value = valueList.get(j);
                valueList.set(j, this.parseValue(value.toString(), dataTypes.get(j)));

            }
        }

        batchInsert(timestamps, measurementsCopy, values, config, strategy, devicePath, mergeTimestampNum);

    }

    private Object parseValue(String rawValue, IotDataType dataType) {
        try {
            return switch (dataType) {
                case INT -> Integer.parseInt(rawValue);
                case LONG -> Long.parseLong(rawValue);
                case FLOAT -> Float.parseFloat(rawValue);
                case DOUBLE -> Double.parseDouble(rawValue);
                case STRING -> rawValue;
                default -> throw new SystemException(CodeMessage.DATA_FORMAT_ERROR,
                        "不支持的Iot数据类型: " + dataType);
            };
        } catch (NumberFormatException e) {
//            throw new SystemException(CodeMessage.DATA_FORMAT_ERROR,
//                    String.format("无法将值 '%s' 解析为类型 %s", rawValue, dataType), e);
            log.warn("无法将值 '{}' 解析为类型 {}", rawValue, dataType);
            return null;
        }
    }

    @LogExecutionTime
    @Override
    public void insertBatchRecord(int deviceId, List<Long> timestamps, String tag,
                                  List<String> measurements, List<List<Object>> values, int mergeTimestampNum) {
        tag = validateTagForInsert(tag);
        List<String> measurementsCopy = addTagInMeasurementsAndValues(tag, measurements, values);
        Device device = deviceService.addDataTagsWithoutCheck(deviceId, Collections.singleton(tag));
        Config config = device.getConfig();
        StorageStrategy strategy = storageStrategyManager.getStrategy(config.getStorageMode());// 获取存储策略
        String devicePath = util.getDevicePath(device.getUserId(), deviceId);

        batchInsert(timestamps, measurementsCopy, values, config, strategy, devicePath, mergeTimestampNum);
    }


    @LogExecutionTime
    @Override
    public void insertBatchRecord(Device device, List<Long> timestamps, String tag,
                                  List<String> measurements, List<List<Object>> values, int mergeTimestampNum) {
        tag = validateTagForInsert(tag);
        List<String> measurementsCopy = addTagInMeasurementsAndValues(tag, measurements, values);
        //如果标签已经存在，则不再添加
        if (!device.getAllTags().contains(tag)) {
            device = deviceService.addDataTagsWithoutCheck(device.getId(), Collections.singleton(tag));
        }
        Config config = device.getConfig();
        StorageStrategy strategy = storageStrategyManager.getStrategy(config.getStorageMode());// 获取存储策略
        String devicePath = util.getDevicePath(device.getUserId(), device.getId());

        batchInsert(timestamps, measurementsCopy, values, config, strategy, devicePath, mergeTimestampNum);
    }

    private List<String> addTagInMeasurementsAndValues(String tag, List<String> measurements, List<List<Object>> values) {
        //此方法会修改原始的values!!!
        List<String> measurementsCopy = new ArrayList<>(measurements);
        if (measurements.contains(Constants.TAG)) {
            throw new BusinessException(CodeMessage.INVALID_TAG_ERROR, "物理量中不能包含TAG");
        }
        measurementsCopy.add(Constants.TAG);
        for (List<Object> valueList : values) {
            valueList.add(tag);
        }

        return measurementsCopy;
    }

    private void batchInsert(List<Long> timestamps, List<String> measurements,
                             List<List<Object>> values, Config config, StorageStrategy strategy, String devicePath,
                             int mergeTimestampNum) {
        //对TAG参数特殊处理
        config.getDataTypes().put(Constants.TAG, IotDataType.STRING);
        // 对齐时间窗口
        Long[] alignedTimeRange = alignTimeRange(timestamps.get(0), timestamps.get(timestamps.size() - 1), config.getAggregationTime());
        log.debug("insertBatchRecord 原始时间范围:{}-{}, 对齐后:{}-{}", timestamps.get(0), timestamps.get(timestamps.size() - 1), alignedTimeRange[0], alignedTimeRange[1]);
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

        log.debug("insertBatchRecord: devicePath={}, timestamps={}, measurements={}, types={}, values={}",
                devicePath, timestamps, measurements, types, values);
    }

    @CacheOp(type = CacheOpType.GET, key = Constants.ALL_ARGS_KEY, prefix = "iot.data")
    @LogExecutionTime(logArgs = true)
    @Override
    public DeviceTable queryRecord(int deviceId, Long startTime, Long endTime,
                                   List<String> selectMeasurements, String tagQuery, Integer aggregationTime,
                                   QueryAggregateFunc queryAggregateFunc, List<List<Double>> thresholds) {
        Device device = deviceService.getDeviceById(deviceId);
        aggregationTime = checkAggregationTime(device, aggregationTime);
        StorageStrategy strategy = storageStrategyManager.getStrategy(device.getConfig().getStorageMode());
        String devicePath = util.getDevicePath(device.getUserId(), deviceId);
        return query(device, startTime, endTime, selectMeasurements, aggregationTime,
                queryAggregateFunc, thresholds, strategy, devicePath, tagQuery);
    }

    @CacheOp(type = CacheOpType.GET, key = Constants.ALL_ARGS_KEY, prefix = "iot.data")
    @LogExecutionTime(logArgs = true)
    @Override
    public DeviceTable queryRecord(Device device, Long startTime, Long endTime,
                                   List<String> selectMeasurements, String tagQuery, Integer aggregationTime,
                                   QueryAggregateFunc queryAggregateFunc, List<List<Double>> thresholds) {
        aggregationTime = checkAggregationTime(device, aggregationTime);
        StorageStrategy strategy = storageStrategyManager.getStrategy(device.getConfig().getStorageMode());
        String devicePath = util.getDevicePath(device.getUserId(), device.getId());
        return query(device, startTime, endTime, selectMeasurements, aggregationTime,
                queryAggregateFunc, thresholds, strategy, devicePath, tagQuery);
    }

    @Override
    public long queryRecordCount(int deviceId, Long startTime, Long endTime) {
        Device device = deviceService.getDeviceById(deviceId);
        return iotDBDao.querySensorsCount(util.getDevicePath(device.getUserId(), deviceId), startTime, endTime);
    }

    private DeviceTable query(Device device, Long startTime, Long endTime,
                              List<String> selectMeasurements, Integer aggregationTime,
                              QueryAggregateFunc queryAggregateFunc, List<List<Double>> thresholds,
                              StorageStrategy strategy, String devicePath, String tagQuery) {
        String[] tags = validateTagForQuery(tagQuery);
        if (ObjectUtil.isEmpty(selectMeasurements)) {
            selectMeasurements = device.getConfig().getMeasurements();
            log.info("queryRecord, selectMeasurements为空，使用设备的配置的所有物理量: {}", selectMeasurements);
        }
        List<String> selectMeasurementsCopy = new ArrayList<>(selectMeasurements);
        // 对齐时间窗口
        Long[] alignedTimeRange = alignTimeRange(startTime, endTime, aggregationTime);
        log.info("queryRecord 原始时间范围:{}-{}, 对齐后:{}-{}", startTime, endTime, alignedTimeRange[0], alignedTimeRange[1]);


        // 获取原始数据
        DeviceTable rawTable;
        //如果此存储策略支持快速聚合查询，则直接查询聚合数据，否则先查询再在此层进行聚合。
        boolean alreadyAggregated = false;
        if (strategy instanceof ExtendFastAggregateQuery && globalConfig.isTryFastAggregate()
                && aggregationTime > 0 && queryAggregateFunc != null) {
            log.info("queryRecord 使用存储策略的快速聚合查询");
            try {
                rawTable = ((ExtendFastAggregateQuery) strategy).fastAggregateQuery(
                        devicePath, alignedTimeRange[0], alignedTimeRange[1], selectMeasurementsCopy,
                        aggregationTime, queryAggregateFunc);
                alreadyAggregated = true;
                log.info("queryRecord 使用存储策略的快速聚合查询完成, " +
                        "共查询到{}条时间戳（包含null的数据行）", rawTable.getRecords().size());
            } catch (Exception e) {
                log.error("queryRecord 使用存储策略的快速聚合查询失败", e);
                log.warn("queryRecord 尝试降级为普通查询");
                if (globalConfig.isFastAggregateErrorMessage()) {
                    messageService.sendSystemMessage(device.getUserId(), "数据快速聚合查询失败",
                            "使用存储策略的快速聚合查询失败，错误：" + e, MessageType.WARNING);
                }
                selectMeasurementsCopy.add(Constants.TAG);//添加TAG列
                rawTable = strategy.retrieveData(
                        devicePath, alignedTimeRange[0], alignedTimeRange[1], selectMeasurementsCopy, aggregationTime
                );
            }
        } else {
            if (aggregationTime > 0 && queryAggregateFunc != null) {
                log.info("queryRecord 存储策略不支持快速聚合查询，降级为普通查询");
                long count = iotDBDao.querySensorsCount(devicePath, alignedTimeRange[0], alignedTimeRange[1]);
                if (count > globalConfig.getAggregateQueryLimitSize()) {
                    throw new BusinessException(CodeMessage.AGGREGATE_QUERY_LIMIT_ERROR,
                            "数据点数量为" + count + "，超过限制" + globalConfig.getAggregateQueryLimitSize());
                }
            }
            selectMeasurementsCopy.add(Constants.TAG);//添加TAG列
            rawTable = strategy.retrieveData(
                    devicePath, alignedTimeRange[0], alignedTimeRange[1], selectMeasurementsCopy, aggregationTime
            );
        }

        // 应用标签过滤
        applyTagFilter(rawTable, tags);

        // 阈值过滤（COUNT模式不处理）
        if (shouldApplyThreshold(queryAggregateFunc, thresholds)) {
            log.debug("queryRecord 应用阈值过滤, selectMeasurements={}, thresholds={}", selectMeasurements, thresholds);
            applyThresholdFilter(rawTable, selectMeasurements, thresholds);
        } else {
            log.debug("queryRecord 不应用阈值过滤, selectMeasurements={}, thresholds={}", selectMeasurements, thresholds);
        }
        DeviceTable aggregatedTable;

        // 查询聚合处理
        if (alreadyAggregated) {
            log.debug("queryRecord 已经聚合过，不再聚合");
            aggregatedTable = rawTable;
        } else if (aggregationTime == 0 || ObjectUtil.isNull(queryAggregateFunc)) {
            log.debug("queryRecord 不聚合, selectMeasurements={}, aggregationTime={}, QueryAggregateFunc={}",
                    selectMeasurements, aggregationTime, queryAggregateFunc);
            aggregatedTable = rawTable;//不聚合直接返回原始数据
        } else {
            log.debug("queryRecord 应用聚合, selectMeasurements={}, aggregationTime={}, QueryAggregateFunc={}",
                    selectMeasurements, aggregationTime, queryAggregateFunc);
            try {
                aggregatedTable = aggregateRawData(rawTable, aggregationTime, queryAggregateFunc);
            } catch (Exception e) {
                log.error("queryRecord 聚合数据失败", e);
                throw new SystemException(CodeMessage.DATA_AGGREGATION_ERROR, "聚合数据失败", e);
            }
        }

        aggregatedTable.setDevicePath(devicePath);
        List<String> types = selectMeasurements.stream()
                .map(m -> device.getConfig().getDataTypes().get(m).toString()).toList();
        aggregatedTable.setTypes(types);

        log.info("queryRecord: device={}, startTime={}, endTime={}, selectMeasurements={}, aggregationTime={}, " +
                        "QueryAggregateFunc={}, thresholds={}, tags={}",
                device, startTime, endTime, selectMeasurements, aggregationTime,
                queryAggregateFunc, thresholds, tags);
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

    private Long[] alignTimeRange(Long start, Long end, int aggregationTime) {
        return new Long[]{
                util.alignToEast8Zone(start, aggregationTime),
                util.alignToEast8Zone(end, aggregationTime)
        };
    }

    //应用标签过滤
    private void applyTagFilter(DeviceTable table, String[] tags) {
        if (tags == null) {
            log.debug("queryRecord 不应用标签过滤");
            return;//不进行过滤
        }
        List<String> tagList = Arrays.asList(tags);
        log.debug("queryRecord 应用标签过滤, tags={}", tagList);

        table.getRecords().replaceAll((timestamp, records) ->
                records.stream()
                        .filter(record -> meetsTagCondition(record, tagList))
                        .collect(Collectors.toList())
        );
        table.purgeEmptyTimestamps();
    }

    private boolean meetsTagCondition(Record record, List<String> tags) {
        Object tagValue = record.getFields().get(Constants.TAG);
        // 检查是否符合任何标签条件,包括NO_TAG
        for (String tag : tags) {
            if (tag.equals(tagValue)) {
                return true;
            }
        }
        // 如果没有匹配任何标签，则不符合条件
        return false;
    }

    @LogExecutionTime
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
        //TAG属性直接取第一个值，相当于FIRST模式。
        measurementValues.forEach((measurement, values) -> {
                    if (Constants.TAG.equals(measurement)) {
                        aggregated.addField(measurement, values.get(0));
                    }
                    aggregated.addField(measurement, performAggregation(values, mode));
                }
        );

        return aggregated;
    }

    //对一个属性对应的一组值应用聚合操作
    private Object performAggregation(List<Object> values, QueryAggregateFunc mode) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        //处理字符串类型，例如TAG属性
        boolean hasString = values.stream().anyMatch(v -> v instanceof String);
        if (hasString) {
            return performAggregationString(values, mode);
        }
        // 过滤掉null值和非数值类型, 并转换为double值
        List<Double> numericValues = values.stream()
                .filter(Objects::nonNull)
                .filter(v -> v instanceof Number)
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
                throw new SystemException(CodeMessage.ILLEGAL_AGGREGATION_ERROR, "不合法的聚合模式: " + mode);
            }
        };
    }

    private Object performAggregationString(List<Object> values, QueryAggregateFunc mode) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        switch (mode) {
            case AVG:
            case FIRST:
                return values.get(0);
            case MIN: {
                List<String> nonNullStrings = values.stream()
                        .filter(Objects::nonNull)
                        .map(v -> (String) v)
                        .toList();
                if (nonNullStrings.isEmpty()) {
                    return null;
                }
                return nonNullStrings.stream()
                        .min(Comparator.comparingInt(String::length))
                        .orElse(null);
            }
            case MAX: {
                List<String> nonNullStrings = values.stream()
                        .filter(Objects::nonNull)
                        .map(v -> (String) v)
                        .toList();
                if (nonNullStrings.isEmpty()) {
                    return null;
                }
                return nonNullStrings.stream()
                        .max(Comparator.comparingInt(String::length))
                        .orElse(null);
            }
            case SUM:
            case COUNT:
                return values.size();
            case LAST:
                return values.get(values.size() - 1);
            default:
                log.error("Illegal aggregation mode for string: {}", mode);
                throw new SystemException(CodeMessage.ILLEGAL_AGGREGATION_ERROR, "不合法的聚合模式: " + mode);
        }
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
        // parallelStream并行流处理提升性能
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

    private String validateTagForInsert(String tag) {
        if (tag == null) {
            return Constants.NO_TAG;//null代表没有标签
        }
        if (Constants.NO_TAG.equals(tag)) {
            throw new BusinessException(CodeMessage.INVALID_TAG_ERROR, "TAG不能为“NO_TAG”");
        }
        if (tag.contains("|")) {
            throw new BusinessException(CodeMessage.INVALID_TAG_ERROR, "TAG包含不合法字符: |");
        }
        return tag;
    }

    private String[] validateTagForQuery(String tagQuery) {
        if (tagQuery == null || tagQuery.isEmpty()) {
            return null;
        }
        if (Constants.NO_TAG.equals(tagQuery)) {
            return new String[]{Constants.NO_TAG};
        }
        String[] tags = tagQuery.split("\\|\\|");
        for (String t : tags) {
            if (t.contains("|")) {
                throw new BusinessException(CodeMessage.INVALID_TAG_ERROR, "TAG包含不合法字符: |");
            }
        }
        return tags;
    }
}
