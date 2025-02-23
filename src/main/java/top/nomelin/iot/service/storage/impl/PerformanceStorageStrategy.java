package top.nomelin.iot.service.storage.impl;

import org.apache.tsfile.enums.TSDataType;
import org.springframework.stereotype.Component;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.dao.IoTDBDao;
import top.nomelin.iot.model.dto.DeviceTable;
import top.nomelin.iot.service.storage.StorageStrategy;
import top.nomelin.iot.util.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PerformanceStorageStrategy implements StorageStrategy {
    private final IoTDBDao iotDBDao;

    public PerformanceStorageStrategy(IoTDBDao iotDBDao) {
        this.iotDBDao = iotDBDao;
    }

    @Override
    public String getTemplateSuffix() {
        return "_PERFORMANCE";
    }

    // TODO 待完成
    @Override
    public void storeData(String devicePath, List<Long> timestamps,
                          List<List<String>> measurementsList, List<List<TSDataType>> typesList,
                          List<List<Object>> valuesList, int aggregationTime, int mergeTimestampNum)  {
        // TODO
        int storageGranularity = util.adjustStorageGranularity(aggregationTime);
        Map<Long, Integer> windowCounters = new HashMap<>();

        List<Long> adjustedTimestamps = new ArrayList<>();
        for (Long originalTs : timestamps) {
            long windowTs = util.alignToStorageWindow(originalTs, storageGranularity);
            int sequence = windowCounters.getOrDefault(windowTs, 0);

            if (sequence >= storageGranularity) {
                throw new BusinessException(CodeMessage.STORAGE_OUT_OF_BOUND_ERROR);
            }

            adjustedTimestamps.add(windowTs);
            windowCounters.put(windowTs, sequence + 1);
        }

        // 直接存储原始数据（每个时间戳对应多个记录）
        iotDBDao.insertBatchAlignedRecordsOfOneDevice(
                devicePath, adjustedTimestamps, measurementsList, typesList, valuesList);
    }

    @Override
    public DeviceTable retrieveData(String devicePath, long startTime, long endTime,
                                    List<String> selectedMeasurements, int aggregationTime) {
        //
        // 直接返回原始存储结构（每个窗口时间戳对应多个记录）
        return iotDBDao.queryRecords(devicePath, startTime, endTime, selectedMeasurements);
    }
}