package top.nomelin.iot.service.storage.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.iotdb.pipe.api.type.Binary;
import org.apache.iotdb.session.template.MeasurementNode;
import org.apache.tsfile.enums.TSDataType;
import org.apache.tsfile.file.metadata.enums.TSEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.stream.Collectors;

@Component
public class CompatibleStorageStrategy implements StorageStrategy {
    private static final Logger log = LoggerFactory.getLogger(CompatibleStorageStrategy.class);
    private final IoTDBDao iotDBDao;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CompatibleStorageStrategy(IoTDBDao iotDBDao) {
        this.iotDBDao = iotDBDao;
    }

    @LogExecutionTime
    @Override
    public List<MeasurementNode> preprocessTemplateNodes(List<MeasurementNode> originalNodes) {
        return originalNodes.stream()
                .map(node -> new MeasurementNode(
                        node.getName(),
                        TSDataType.TEXT,  // 强制转为TEXT
                        TSEncoding.PLAIN,// 强制转为PLAIN
                        Constants.COMPRESSION_TYPE))
                .collect(Collectors.toList());
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
        log.info("调整存储粒度，从{}调整到{}", aggregationTime, storageGranularity);
        Map<Long, Map<String, List<Object>>> windowData = new HashMap<>();

        // 聚合数据到调整后的时间窗口
        for (int i = 0; i < timestamps.size(); i++) {
            long originalTs = timestamps.get(i);//原始时间戳
            //调整后的时间戳,也就是一个聚合粒度的开始时间戳
            long windowTs = util.alignToStorageWindow(originalTs, storageGranularity);
            //measurements是一个时间粒度内聚合后的数据行
            Map<String, List<Object>> measurements = windowData.computeIfAbsent(windowTs, k -> new HashMap<>());
            List<String> ms = measurementsList.get(i);
            List<Object> vs = valuesList.get(i);

            // 合并同窗口数据
            for (int j = 0; j < ms.size(); j++) {
                measurements.computeIfAbsent(ms.get(j), k -> new ArrayList<>()).add(vs.get(j));
            }
        }
        log.info("对齐时间戳，从{}调整到{}", timestamps, windowData.keySet());

        // 转换为单条记录存储（JSON数组）
        List<Long> storedTimestamps = new ArrayList<>();//聚合后的时间戳
        List<List<String>> storedMeasurements = new ArrayList<>();
        List<List<TSDataType>> storedTypes = new ArrayList<>();// 转为TEXT
        List<List<Object>> storedValues = new ArrayList<>();// 转为JSON字符串

        windowData.forEach((windowTs, measurements) -> {
            storedTimestamps.add(windowTs);//聚合后的时间戳
            List<String> ms = new ArrayList<>();
            List<TSDataType> ts = new ArrayList<>();
            List<Object> vs = new ArrayList<>();

            measurements.forEach((measurement, values) -> {
                ms.add(measurement);
                ts.add(TSDataType.TEXT);// 转为TEXT
                try {
                    vs.add(objectMapper.writeValueAsString(values));// 转为JSON字符串
                } catch (JsonProcessingException e) {
                    throw new SystemException(CodeMessage.JSON_WRITE_ERROR);
                }
            });

            storedMeasurements.add(ms);
            storedTypes.add(ts);
            storedValues.add(vs);
        });

        iotDBDao.insertBatchAlignedRecordsOfOneDevice(
                devicePath, storedTimestamps, storedMeasurements, storedTypes, storedValues);
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

/*    public void testProcessStoredRecord() {
        // 临时创建一个模拟的 DeviceTable 和 Record
        DeviceTable resultTable = new DeviceTable();
        long storedTimestamp = 1623245678000L; // 模拟时间戳

        // 模拟一个 Record（包含温度和湿度的压缩存储数据）
        Record storedRecord = new Record();

        // 模拟温度和湿度的 JSON 列表值
        List<Object> temperatureValues = List.of(16.0, 17.5, 15.0); // 模拟温度的值
        List<Object> humidityValues = List.of(65.0, 65.0, 70.0);    // 模拟湿度的值

        // 将这些值作为 JSON 存储到 Record 的 fields 中
        try {
            storedRecord.getFields().put("温度", new ObjectMapper().writeValueAsString(temperatureValues));
            storedRecord.getFields().put("湿度", new ObjectMapper().writeValueAsString(humidityValues));
        } catch (JsonProcessingException e) {
            e.printStackTrace(); // 处理 JSON 序列化异常
        }

        // 调用 processStoredRecord 方法来处理这些记录
        processStoredRecord(storedTimestamp, storedRecord, resultTable);

        // 输出结果以检查是否正确
        resultTable.getRecords().forEach((timestamp, records) -> {
            System.out.println("时间戳: " + timestamp);
            records.forEach(record -> {
                record.getFields().forEach((measurement, value) -> {
                    System.out.println("测量项: " + measurement + ", 值: " + value);
                });
                System.out.println("----------");
            });
        });
    }
    public static void main(String[] args) {
        CompatibleStorageStrategy compatibleStorageStrategy = new CompatibleStorageStrategy(null);
        compatibleStorageStrategy.testProcessStoredRecord();
    }*/
}