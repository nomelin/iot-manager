package top.nomelin.iot.service.storage.impl;

import org.apache.tsfile.enums.TSDataType;
import org.springframework.stereotype.Component;
import top.nomelin.iot.dao.IoTDBDao;
import top.nomelin.iot.model.DeviceTable;
import top.nomelin.iot.service.storage.StorageStrategy;
import top.nomelin.iot.util.util;

import java.util.ArrayList;
import java.util.List;

@Component
public class PerformanceStorageStrategy implements StorageStrategy {
    private final IoTDBDao iotDBDao;

    public PerformanceStorageStrategy(IoTDBDao iotDBDao) {
        this.iotDBDao = iotDBDao;
    }

    @Override
    public void storeData(String devicePath, List<Long> timestamps,
                          List<List<String>> measurementsList, List<List<TSDataType>> typesList,
                          List<List<Object>> valuesList, int aggregationTime) {
        // 调整存储粒度
        int adjustedGranularity = util.adjustStorageGranularity(aggregationTime);
        List<Long> adjustedTimestamps = new ArrayList<>();

        for (Long timestamp : timestamps) {
            // 计算时间基数（去掉末位多余的精度）
            long base = timestamp / adjustedGranularity * adjustedGranularity;
            // 保留精度范围内的偏移量//TODO 这样加回去和原来的时间戳一样?
            long offset = timestamp % adjustedGranularity;
            adjustedTimestamps.add(base + offset);
        }

        iotDBDao.insertBatchAlignedRecordsOfOneDevice(
                devicePath, adjustedTimestamps, measurementsList, typesList, valuesList);
    }

    @Override
    public DeviceTable retrieveData(String devicePath, long startTime, long endTime,
                                    List<String> selectedMeasurements, int aggregationTime) {
        // 获取调整后的存储粒度
        int storageGranularity = util.adjustStorageGranularity(aggregationTime);

        DeviceTable rawTable = selectedMeasurements == null ?
                iotDBDao.queryRecords(devicePath, startTime, endTime) :
                iotDBDao.queryRecords(devicePath, startTime, endTime, selectedMeasurements);

        DeviceTable resultTable = new DeviceTable();
        rawTable.getRecords().forEach((adjustedTs, record) -> {
            // 反向计算原始时间戳
            long base = adjustedTs / storageGranularity * storageGranularity;
            long offset = adjustedTs % storageGranularity;
            long originalTs = base + offset;
            resultTable.getRecords().put(originalTs, record);
        });

        return resultTable;
    }
}