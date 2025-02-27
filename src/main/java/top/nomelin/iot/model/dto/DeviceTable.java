package top.nomelin.iot.model.dto;


import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.tsfile.read.common.RowRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 一个设备的查询结果
 */
public class DeviceTable {
    private Map<Long, List<Record>> records;//时间戳和Record的映射
    private String devicePath;
    private List<String> types;

    public DeviceTable() {
        records = new HashMap<>();
    }

    /**
     * 将IoTDB定义的SessionDataSet转换为DeviceTable
     *
     * @param dataSet    查询结果
     * @param devicePath 设备路径
     * @return DeviceTable
     */
    public static DeviceTable convertToDeviceTable(SessionDataSet dataSet, String devicePath) throws IoTDBConnectionException, StatementExecutionException {
        DeviceTable deviceTable = new DeviceTable();
        deviceTable.devicePath = devicePath;
        deviceTable.types = dataSet.getColumnTypes().subList(1, dataSet.getColumnTypes().size());// 去掉时间戳列
        List<String> columnNames = dataSet.getColumnNames().subList(1, dataSet.getColumnNames().size());// 去掉时间戳列
        //去掉属性名称的前缀
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            if (columnName.startsWith(devicePath + ".")) {
                columnNames.set(i, columnName.substring(devicePath.length() + 1));
            }
        }
        // 转换为Record
        while (dataSet.hasNext()) {
            RowRecord rowRecord = dataSet.next();
            Record record = Record.convertToRowRecord(rowRecord, columnNames);
            deviceTable.addRecord(rowRecord.getTimestamp(), record);
        }
        return deviceTable;
    }

    /**
     * 添加记录到指定时间戳
     */
    public void addRecord(long timestamp, Record record) {
        records.computeIfAbsent(timestamp, k -> new ArrayList<>()).add(record);
    }

    /**
     * 清除没有Record的时间戳
     */
    public void purgeEmptyTimestamps() {
        records.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public String getDevicePath() {
        return devicePath;
    }

    public void setDevicePath(String devicePath) {
        this.devicePath = devicePath;
    }

    public List<String> getTypes() {
        return types;
    }

    public void setTypes(List<String> types) {
        this.types = types;
    }

    public Map<Long, List<Record>> getRecords() {
        return records;
    }

    public void setRecords(Map<Long, List<Record>> records) {
        this.records = records;
    }

    public List<Record> getRecords(long timestamp) {
        return records.get(timestamp);
    }

    @Override
    public String toString() {
        return "DeviceTable{" +
                "records=" + records +
                ", devicePath='" + devicePath + '\'' +
                ", types=" + types +
                '}';
    }
}
