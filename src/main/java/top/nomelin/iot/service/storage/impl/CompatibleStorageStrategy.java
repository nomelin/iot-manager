package top.nomelin.iot.service.storage.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.iotdb.pipe.api.type.Binary;
import org.apache.iotdb.session.template.MeasurementNode;
import org.apache.tsfile.enums.TSDataType;
import org.apache.tsfile.file.metadata.enums.TSEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.nomelin.iot.common.Constants;
import top.nomelin.iot.common.annotation.LogExecutionTime;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.SystemException;
import top.nomelin.iot.dao.IoTDBDao;
import top.nomelin.iot.model.dto.DeviceTable;
import top.nomelin.iot.model.dto.Record;
import top.nomelin.iot.service.storage.StorageStrategy;
import top.nomelin.iot.util.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CompatibleStorageStrategy implements StorageStrategy {
    private static final Logger log = LoggerFactory.getLogger(CompatibleStorageStrategy.class);
    private final IoTDBDao iotDBDao;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${iotdb.storage.compatible.dont-merge:false}")
    private boolean DONT_MERGE;

    public CompatibleStorageStrategy(IoTDBDao iotDBDao) {
        this.iotDBDao = iotDBDao;
    }

    @LogExecutionTime
    @Override
    public List<MeasurementNode> preprocessTemplateNodes(List<MeasurementNode> originalNodes) {
        List<MeasurementNode> resultNodes = originalNodes.stream()
                .map(node -> new MeasurementNode(
                        node.getName(),
                        TSDataType.TEXT,  // 强制转为TEXT
                        TSEncoding.PLAIN,// 强制转为PLAIN
                        Constants.COMPRESSION_TYPE))
                .toList();
        log.info("COMPATIBLE: 模板节点从 {} 转换为 {}",
                originalNodes.stream().map(MeasurementNode::getName).toList(),
                resultNodes.stream().map(MeasurementNode::getName).toList());
        return resultNodes;
    }

    @Override
    public String getTemplateSuffix() {
        return "_COMPATIBLE";
    }

    @LogExecutionTime
    @Override
    public void storeData(String devicePath, List<Long> timestamps,
                          List<List<String>> measurementsList, List<List<TSDataType>> typesList,
                          List<List<Object>> valuesList, int aggregationTime) {
        // 调整存储粒度
        int storageGranularity = util.adjustStorageGranularity(aggregationTime);
        log.info("Adjusted storage granularity from {} to {}", aggregationTime, storageGranularity);

        // 聚合当前批次数据到时间窗口
        Map<Long, Map<String, List<Object>>> windowData = aggregateCurrentBatch(
                timestamps, measurementsList, valuesList, storageGranularity);

        if (DONT_MERGE) {
            log.info("DONT_MERGE is true, skip merging existing data");
        } else {
            log.info("DONT_MERGE is false, start merging existing data");
            mergeExistingData(devicePath, windowData);
        }

        // 生成存储记录
        StorageRecords records = generateStorageRecords(windowData);

        // 存储到IoTDB
        iotDBDao.insertBatchAlignedRecordsOfOneDevice(
                devicePath, records.timestamps(), records.measurements(), records.types(), records.values());
    }

    private Map<Long, Map<String, List<Object>>> aggregateCurrentBatch(List<Long> timestamps,
                                                                       List<List<String>> measurementsList,
                                                                       List<List<Object>> valuesList,
                                                                       int storageGranularity) {
        Map<Long, Map<String, List<Object>>> windowData = new HashMap<>();

        // 聚合数据到调整后的时间窗口
        for (int i = 0; i < timestamps.size(); i++) {
            long originalTs = timestamps.get(i);//原始时间戳
            //调整后的时间戳,也就是一个聚合粒度的开始时间戳
            long windowTs = util.alignToStorageWindow(originalTs, storageGranularity);
            List<String> ms = measurementsList.get(i);
            List<Object> vs = valuesList.get(i);
            Map<String, List<Object>> measurements = windowData.computeIfAbsent(windowTs, k -> new HashMap<>());

            for (int j = 0; j < ms.size(); j++) {
                String measurement = ms.get(j);
                Object value = vs.get(j);
                measurements.computeIfAbsent(measurement, k -> new ArrayList<>()).add(value);
            }
        }
        log.info("Aligned timestamps from {} to windows: {}", timestamps, windowData.keySet());
        return windowData;
    }

    private void mergeExistingData(String devicePath, Map<Long, Map<String, List<Object>>> windowData) {
        windowData.forEach((windowTs, currentMeasurements) -> {
            Map<String, String> existingData = iotDBDao.getExistingMeasurements(devicePath, windowTs);
            existingData.forEach((measurement, jsonValue) -> {
                try {
//                    log.info("currentMeasurements: {}, measurement: {}, jsonValue: {}", currentMeasurements, measurement, jsonValue);
                    if (jsonValue == null) {
                        log.info("!!!!!!!!!!!!jsonValue is null");
                    }
                    List<?> existingValues = objectMapper.readValue(jsonValue, List.class);
                    List<Object> currentValues = currentMeasurements.get(measurement);
                    if (currentValues == null) {
                        //如果此物理量没有新数据，则直接使用旧数据回写。
                        currentMeasurements.put(measurement, new ArrayList<>(existingValues));
                    } else {
                        List<Object> merged = new ArrayList<>(existingValues);
                        merged.addAll(currentValues);
                        currentMeasurements.put(measurement, merged);
                    }
                } catch (JsonProcessingException e) {
                    log.error("Failed to merge data for {} at {}: {}", measurement, windowTs, e.getMessage());
                    throw new SystemException(CodeMessage.JSON_READ_ERROR,
                            "Failed to merge data for " + measurement + " at " + windowTs, e);
                }
            });
        });
    }

    private StorageRecords generateStorageRecords(Map<Long, Map<String, List<Object>>> windowData) {
        // 转换为单条记录存储（JSON数组）
        List<Long> storedTimestamps = new ArrayList<>();//聚合后的时间戳
        List<List<String>> storedMeasurements = new ArrayList<>();
        List<List<TSDataType>> storedTypes = new ArrayList<>();// 转为TEXT
        List<List<Object>> storedValues = new ArrayList<>();// 转为JSON字符串

        windowData.forEach((windowTs, measurements) -> {
            List<String> msList = new ArrayList<>();
            List<TSDataType> typeList = new ArrayList<>();
            List<Object> valueList = new ArrayList<>();

            measurements.forEach((measurement, values) -> {
                msList.add(measurement);
                typeList.add(TSDataType.TEXT);
                valueList.add(serializeValues(values));
            });

            storedTimestamps.add(windowTs);
            storedMeasurements.add(msList);
            storedTypes.add(typeList);
            storedValues.add(valueList);
        });

        return new StorageRecords(storedTimestamps, storedMeasurements, storedTypes, storedValues);
    }

    private String serializeValues(List<Object> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new SystemException(CodeMessage.JSON_WRITE_ERROR, "values:" + values.toString(), e);
        }
    }

    @LogExecutionTime
    @Override
    public DeviceTable retrieveData(String devicePath, long startTime, long endTime,
                                    List<String> selectedMeasurements, int aggregationTime) {
        DeviceTable resultTable = new DeviceTable();
        DeviceTable rawTable = selectedMeasurements == null ?
                iotDBDao.queryRecords(devicePath, startTime, endTime) :
                iotDBDao.queryRecords(devicePath, startTime, endTime, selectedMeasurements);
        for (Map.Entry<Long, List<Record>> entry : rawTable.getRecords().entrySet()) {
            long storedTimestamp = entry.getKey();
            //兼容模式下，同一个时间戳只会有一个记录。
            if (entry.getValue().size() != 1) {
                throw new SystemException(CodeMessage.DUPLICATE_TIME_ERROR);
            }
            Record storedRecord = entry.getValue().get(0);
            processStoredRecord(storedTimestamp, storedRecord, resultTable);
        }

        return resultTable;
    }

    private void processStoredRecord(long storedTimestamp, Record storedRecord, DeviceTable resultTable) {
        List<Record> records = new ArrayList<>();
        storedRecord.getFields().forEach((measurement, jsonValue) -> {
            try {
                String jsonString = (jsonValue instanceof Binary) ?
                        ((Binary) jsonValue).getStringValue() : // Tsfile 1.x 的获取方式
                        jsonValue.toString(); // 兼容处理其他类型
                List<?> values = objectMapper.readValue(jsonString, List.class);
                //第一次得到values，创建对应数量的 Record
                if (records.isEmpty()) {
                    for (int i = 0; i < values.size(); i++) {
                        records.add(new Record());
                    }
                }
                //将一个压缩的属性List还原到多个Record中
                for (int i = 0; i < values.size(); i++) {
                    records.get(i).getFields().put(measurement, values.get(i));
                }
            } catch (IOException e) {
                throw new SystemException(CodeMessage.JSON_READ_ERROR, "Failed to deserialize measurement: " + measurement, e);
            }
        });
        //将这些 Record 放入 DeviceTable 中
        for (Record record : records) {
            resultTable.addRecord(storedTimestamp, record);
        }
    }

    //只是用于内部传递返回值
    private record StorageRecords(List<Long> timestamps,
                                  List<List<String>> measurements,
                                  List<List<TSDataType>> types,
                                  List<List<Object>> values) {
    }
}