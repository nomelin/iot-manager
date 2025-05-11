package top.nomelin.iot.service.storage.impl;

import org.apache.tsfile.enums.TSDataType;
import org.apache.tsfile.utils.Binary;
import org.springframework.stereotype.Component;
import top.nomelin.iot.cache.CacheOperations;
import top.nomelin.iot.cache.CacheResult;
import top.nomelin.iot.common.annotation.LogExecutionTime;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.dao.IoTDBDao;
import top.nomelin.iot.model.dto.DeviceTable;
import top.nomelin.iot.model.enums.QueryAggregateFunc;
import top.nomelin.iot.service.storage.ExtendFastAggregateQuery;
import top.nomelin.iot.service.storage.StorageStrategy;
import top.nomelin.iot.util.util;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class PerformanceStorageStrategy implements StorageStrategy, ExtendFastAggregateQuery {
    private final IoTDBDao iotDBDao;
    private final CacheOperations<String, Integer> windowCounterCache;

    public PerformanceStorageStrategy(IoTDBDao iotDBDao,
                                      CacheOperations<String, Integer> windowCounterCache) {
        this.iotDBDao = iotDBDao;
        this.windowCounterCache = windowCounterCache;
    }

    @Override
    public String getTemplateSuffix() {
        return "_PERFORMANCE";
    }

    @Override
    public void storeData(String devicePath, List<Long> timestamps,
                          List<List<String>> measurementsList, List<List<TSDataType>> typesList,
                          List<List<Object>> valuesList, int aggregationTime, int mergeTimestampNum) {
        int storageGranularity = util.adjustStorageGranularity(aggregationTime);
        List<Long> adjustedTimestamps = new ArrayList<>(timestamps.size());

        for (long originalTs : timestamps) {
            long windowTs = util.alignToStorageWindow(originalTs, storageGranularity);
            String cacheKey = buildCacheKey(devicePath, windowTs);

            // 获取当前窗口序列号
            int sequence = getCurrentSequence(devicePath, storageGranularity, windowTs, cacheKey);

            // 生成带低位序列号的时间戳
            long adjustedTs = windowTs + sequence;
            adjustedTimestamps.add(adjustedTs);

            // 更新缓存中的序列号
            windowCounterCache.put(cacheKey, sequence + 1);
        }

        // 批量插入调整后的数据
        iotDBDao.insertBatchAlignedRecordsOfOneDevice(
                devicePath, adjustedTimestamps, measurementsList, typesList, valuesList);
    }

    @LogExecutionTime
    @Override
    public DeviceTable retrieveData(String devicePath, Long startTime, Long endTime,
                                    List<String> selectedMeasurements, int aggregationTime) {
        // 直接查询原始存储结构（每个调整后的时间戳对应一个记录）
        DeviceTable table = selectedMeasurements == null ?
                iotDBDao.queryRecords(devicePath, startTime, endTime) :
                iotDBDao.queryRecords(devicePath, startTime, endTime, selectedMeasurements);
        // 后处理二进制字段
        if (table != null) {
            table.getRecords().forEach((timestamp, records) ->
                    records.forEach(record ->
                            record.getFields().replaceAll((key, value) ->
                                    value instanceof Binary ?
                                            ((Binary) value).getStringValue(StandardCharsets.UTF_8) :
                                            value
                            )
                    )
            );
        }
        return table;
    }

    @LogExecutionTime
    @Override
    public DeviceTable fastAggregateQuery(String devicePath, Long startTime, Long endTime, List<String> selectedMeasurements,
                                          int aggregationTime, QueryAggregateFunc queryAggregateFunc) {
        if (selectedMeasurements == null) {
            throw new BusinessException(CodeMessage.DATA_AGGREGATION_ERROR, "快速聚合查询时，必须指定聚合的字段");
        }
        DeviceTable table = iotDBDao.queryAggregatedRecords(devicePath, startTime, endTime,
                selectedMeasurements, aggregationTime, queryAggregateFunc);
        //二进制字段不能聚合。
        return table;
    }

    private int getCurrentSequence(String devicePath, int storageGranularity,
                                   long windowTs, String cacheKey) {
        // 尝试从缓存获取序列号
        CacheResult<Integer> result = windowCounterCache.get(cacheKey);

        if (result.isHit() && result.getData() != null) {
            // 检查窗口容量
            if (result.getData() >= storageGranularity) {
                throw new BusinessException(CodeMessage.STORAGE_OUT_OF_BOUND_ERROR);
            }
            return result.getData();
        }

        // 缓存未命中时查询数据库获取当前窗口记录数
        long count = iotDBDao.queryRecordsCount(
                devicePath,
                windowTs,
                windowTs + storageGranularity
        );
        int sequence = (int) count;

        // 检查窗口容量
        if (sequence >= storageGranularity) {
            throw new BusinessException(CodeMessage.STORAGE_OUT_OF_BOUND_ERROR);
        }

        // 初始化缓存值
        windowCounterCache.put(cacheKey, sequence);
        return sequence;
    }

    private String buildCacheKey(String devicePath, long windowTs) {
        return String.format("%s@%d", devicePath, windowTs);
    }


}