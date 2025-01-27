package top.nomelin.iot.service.storage.impl;

import org.apache.tsfile.enums.TSDataType;
import org.springframework.stereotype.Component;
import top.nomelin.iot.dao.IoTDBDao;
import top.nomelin.iot.model.DeviceTable;
import top.nomelin.iot.service.storage.StorageStrategy;

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
        List<Long> adjustedTimestamps = new ArrayList<>();

        for (Long timestamp : timestamps) {
            long base = timestamp / aggregationTime * aggregationTime;
            long offset = timestamp % aggregationTime;
            adjustedTimestamps.add(base + offset / (aggregationTime / 1000));
        }

        iotDBDao.insertBatchAlignedRecordsOfOneDevice(
                devicePath, adjustedTimestamps, measurementsList, typesList, valuesList);
    }

    @Override
    public DeviceTable retrieveData(String devicePath, long startTime, long endTime,
                                    List<String> selectedMeasurements, int aggregationTime) {
        DeviceTable rawTable = iotDBDao.queryRecords(devicePath, startTime, endTime, selectedMeasurements);
        DeviceTable resultTable = new DeviceTable();

        rawTable.getRecords().forEach((adjustedTs, record) -> {
            long base = adjustedTs / aggregationTime * aggregationTime;
            long offset = adjustedTs % aggregationTime;
            long originalTs = base + offset * (aggregationTime / 1000);

            resultTable.getRecords().put(originalTs, record);
        });

        return resultTable;
    }
}