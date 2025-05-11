package top.nomelin.iot.service.storage.impl;

import org.apache.tsfile.enums.TSDataType;
import org.apache.tsfile.utils.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
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
import java.util.List;

@Component
public class CoverStorageStrategy implements StorageStrategy, ExtendFastAggregateQuery {
    private static final Logger log = LoggerFactory.getLogger(CoverStorageStrategy.class);
    private final IoTDBDao iotDBDao;

    public CoverStorageStrategy(IoTDBDao iotDBDao) {
        this.iotDBDao = iotDBDao;
    }

    @Override
    public String getTemplateSuffix() {
        return "_COVER";
    }

    @LogExecutionTime
    @Override
    public void storeData(String devicePath, List<Long> timestamps,
                          List<List<String>> measurementsList, List<List<TSDataType>> typesList,
                          List<List<Object>> valuesList, int aggregationTime, int mergeTimestampNum) {
        // 调整存储粒度
        int storageGranularity = util.adjustStorageGranularity(aggregationTime);
        log.debug("调整存储粒度，从{}调整到{}", aggregationTime, storageGranularity);
        // 对齐时间戳
        List<Long> alignedTimestamps = util.alignTimestamps(timestamps, storageGranularity);
        log.debug("对齐时间戳，从{}调整到{}", timestamps, alignedTimestamps);
        // 写入原始数据
        iotDBDao.insertBatchAlignedRecordsOfOneDevice(
                devicePath, alignedTimestamps, measurementsList, typesList, valuesList);
    }

    @LogExecutionTime
    @Override
    public DeviceTable retrieveData(String devicePath, Long startTime, Long endTime,
                                    List<String> selectedMeasurements, int aggregationTime) {
        //直接查询即可
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
}
