package top.nomelin.iot.model.dto;


import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.tsfile.read.common.RowRecord;

import java.util.*;

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
     * 将IoTDB定义的聚合查询的SessionDataSet转换为DeviceTable
     * 这个方法会过滤掉所有字段全为null的Record。
     * 如果一个Record有字段是null，有字段不是null，则会保留。
     *
     * @param dataSet    查询结果
     * @param devicePath 设备路径
     * @return DeviceTable
     */
    public static DeviceTable convertAggregatedDataToDeviceTable(SessionDataSet dataSet, String devicePath)
            throws IoTDBConnectionException, StatementExecutionException {
        DeviceTable deviceTable = new DeviceTable();
        deviceTable.devicePath = devicePath;
        deviceTable.types = dataSet.getColumnTypes().subList(1, dataSet.getColumnTypes().size()); // 去掉时间列

        // 提取字段名称，例如从 AVG(root.x.y.z) 提取 z
        List<String> columnNames = dataSet.getColumnNames().subList(1, dataSet.getColumnNames().size());
        for (int i = 0; i < columnNames.size(); i++) {
            String col = columnNames.get(i);
            int lastDotIndex = col.lastIndexOf('.');
            int parenIndex = col.indexOf('(');
            int endParenIndex = col.indexOf(')');
            // 截取 AVG(root.x.y.z) → z
            if (parenIndex >= 0 && lastDotIndex > parenIndex && endParenIndex > lastDotIndex) {
                columnNames.set(i, col.substring(lastDotIndex + 1, endParenIndex));
            } else {
                // 兜底，尝试提取最后一段
                columnNames.set(i, col.substring(lastDotIndex + 1));
            }
        }

        // 转换为 Record
        // 并过滤全 null 行
        while (dataSet.hasNext()) {
            RowRecord rowRecord = dataSet.next();
            Record record = Record.convertToRowRecord(rowRecord, columnNames);
            // 判断是否所有字段都是 null
            boolean allNull = record.getFields().values().stream().allMatch(Objects::isNull);
            if (!allNull) {
                deviceTable.addRecord(rowRecord.getTimestamp(), record);
            }
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
