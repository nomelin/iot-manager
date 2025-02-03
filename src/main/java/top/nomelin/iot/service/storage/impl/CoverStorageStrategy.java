package top.nomelin.iot.service.storage.impl;

import org.apache.tsfile.enums.TSDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import top.nomelin.iot.dao.IoTDBDao;
import top.nomelin.iot.model.DeviceTable;
import top.nomelin.iot.service.storage.StorageStrategy;
import top.nomelin.iot.util.util;

import java.util.List;

@Component
public class CoverStorageStrategy implements StorageStrategy {
    private static final Logger log = LoggerFactory.getLogger(CoverStorageStrategy.class);
    private final IoTDBDao iotDBDao;

    public CoverStorageStrategy(IoTDBDao iotDBDao) {
        this.iotDBDao = iotDBDao;
    }

    @Override
    public String getTemplateSuffix() {
        return "_COVER";
    }

    @Override
    public void storeData(String devicePath, List<Long> timestamps,
                          List<List<String>> measurementsList, List<List<TSDataType>> typesList,
                          List<List<Object>> valuesList, int aggregationTime) {
        // 调整存储粒度
        int storageGranularity = util.adjustStorageGranularity(aggregationTime);
        log.info("调整存储粒度，从{}调整到{}", aggregationTime, storageGranularity);
        // 对齐时间戳
        List<Long> alignedTimestamps = util.alignTimestamps(timestamps, storageGranularity);
        log.info("对齐时间戳，从{}调整到{}", timestamps, alignedTimestamps);
        // 写入原始数据
        iotDBDao.insertBatchAlignedRecordsOfOneDevice(
                devicePath, alignedTimestamps, measurementsList, typesList, valuesList);
    }

    @Override
    public DeviceTable retrieveData(String devicePath, long startTime, long endTime,
                                    List<String> selectedMeasurements, int aggregationTime) {
        //直接查询即可
        return selectedMeasurements == null ?
                iotDBDao.queryRecords(devicePath, startTime, endTime) :
                iotDBDao.queryRecords(devicePath, startTime, endTime, selectedMeasurements);
    }
}
