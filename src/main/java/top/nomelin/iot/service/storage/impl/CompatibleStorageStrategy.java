package top.nomelin.iot.service.storage.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tsfile.enums.TSDataType;
import org.springframework.stereotype.Component;
import top.nomelin.iot.dao.IoTDBDao;
import top.nomelin.iot.model.DeviceTable;
import top.nomelin.iot.model.Record;
import top.nomelin.iot.service.storage.StorageStrategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CompatibleStorageStrategy implements StorageStrategy {
    private final IoTDBDao iotDBDao;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CompatibleStorageStrategy(IoTDBDao iotDBDao) {
        this.iotDBDao = iotDBDao;
    }

    @Override
    public void storeData(String devicePath, List<Long> timestamps,
                          List<List<String>> measurementsList, List<List<TSDataType>> typesList,
                          List<List<Object>> valuesList, int aggregationTime) {
        Map<Long, Map<String, List<Object>>> aggregatedData = new HashMap<>();

        // 聚合数据到时间窗口
        for (int i = 0; i < timestamps.size(); i++) {
            long timestamp = timestamps.get(i);
            long windowStart = timestamp / aggregationTime * aggregationTime;

            Map<String, List<Object>> windowData = aggregatedData.computeIfAbsent(
                    windowStart, k -> new HashMap<>());

            List<String> measurements = measurementsList.get(i);
            List<Object> values = valuesList.get(i);

            for (int j = 0; j < measurements.size(); j++) {
                String measurement = measurements.get(j);
                Object value = values.get(j);
                windowData.computeIfAbsent(measurement, k -> new ArrayList<>()).add(value);
            }
        }

        // 转换为JSON存储
        List<Long> storedTimestamps = new ArrayList<>();
        List<List<String>> storedMeasurements = new ArrayList<>();
        List<List<TSDataType>> storedTypes = new ArrayList<>();
        List<List<Object>> storedValues = new ArrayList<>();

        aggregatedData.forEach((windowStart, measurementsMap) -> {
            storedTimestamps.add(windowStart);
            List<String> ms = new ArrayList<>();
            List<TSDataType> ts = new ArrayList<>();
            List<Object> vs = new ArrayList<>();

            measurementsMap.forEach((measurement, values) -> {
                ms.add(measurement);
                ts.add(TSDataType.TEXT);
                try {
                    vs.add(objectMapper.writeValueAsString(values));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("JSON序列化失败", e);
                }
            });

            storedMeasurements.add(ms);
            storedTypes.add(ts);
            storedValues.add(vs);
        });

        iotDBDao.insertBatchAlignedRecordsOfOneDevice(
                devicePath, storedTimestamps, storedMeasurements, storedTypes, storedValues);
    }

    @Override
    public DeviceTable retrieveData(String devicePath, long startTime, long endTime,
                                    List<String> selectedMeasurements, int aggregationTime) {
        DeviceTable rawTable = iotDBDao.queryRecords(devicePath, startTime, endTime, selectedMeasurements);
        DeviceTable resultTable = new DeviceTable();

        rawTable.getRecords().forEach((timestamp, record) -> {
            record.getFields().forEach((measurement, value) -> {
                try {
                    List<Object> values = objectMapper.readValue((String)value, new TypeReference<>() {});
                    // 将数据展开到原始时间粒度
                    values.forEach((v, index) -> {
                        long originalTs = timestamp + index * (aggregationTime / values.size());
                        resultTable.getRecords()
                                .computeIfAbsent(originalTs, k -> new Record())
                                .getFields().put(measurement, v);
                    });
                } catch (IOException e) {
                    throw new RuntimeException("JSON解析失败", e);
                }
            });
        });

        return resultTable;
    }
}


