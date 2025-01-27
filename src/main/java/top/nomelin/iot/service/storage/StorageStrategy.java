package top.nomelin.iot.service.storage;

import org.apache.tsfile.enums.TSDataType;
import top.nomelin.iot.model.DeviceTable;

import java.util.List;

public interface StorageStrategy {
    // 存储数据的方法
    void storeData(String devicePath, List<Long> timestamps, List<List<String>> measurementsList,
                   List<List<TSDataType>> typesList, List<List<Object>> valuesList, int aggregationTime);

    // 读取数据的方法
    DeviceTable retrieveData(String devicePath, long startTime, long endTime, List<String> selectedMeasurements,
                             int aggregationTime);
}

