package top.nomelin.iot.service.storage;

import org.apache.tsfile.enums.TSDataType;
import top.nomelin.iot.model.DeviceTable;

import java.util.List;

public interface StorageStrategy {
    /**
     * 按一定策略存储数据
     *
     * @param devicePath       设备路径
     * @param timestamps       时间戳列表
     * @param measurementsList 物理量列表
     * @param typesList        数据类型列表
     * @param valuesList       数据值列表
     * @param aggregationTime  聚合时间粒度，必须是10的整数次幂，比如1，10，100，单位为ms。
     */
    void storeData(String devicePath, List<Long> timestamps, List<List<String>> measurementsList,
                   List<List<TSDataType>> typesList, List<List<Object>> valuesList, int aggregationTime);

    /**
     * 按和存储时相同的策略查询数据
     *
     * @param devicePath           设备路径
     * @param startTime            起始时间戳
     * @param endTime              结束时间戳
     * @param selectedMeasurements 要查询的物理量
     * @param aggregationTime      聚合时间粒度，必须是10的整数次幂，比如1，10，100，单位为ms。
     * @return 查询到的数据
     */
    DeviceTable retrieveData(String devicePath, long startTime, long endTime, List<String> selectedMeasurements,
                             int aggregationTime);
}

