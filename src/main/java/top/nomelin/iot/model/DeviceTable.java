package top.nomelin.iot.model;


import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.tsfile.read.common.RowRecord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceTable {
    private final Map<Long, Record> records;
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
            deviceTable.records.put(rowRecord.getTimestamp(), record);
        }
        return deviceTable;
    }

    public String getDevicePath() {
        return devicePath;
    }

    public List<String> getTypes() {
        return types;
    }

    public Map<Long, Record> getRecords() {
        return records;
    }

    public Record getRecord(long timestamp) {
        return records.get(timestamp);
    }

}
