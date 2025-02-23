package top.nomelin.iot.dao.impl;

import org.apache.iotdb.isession.SessionDataSet;
import org.apache.iotdb.isession.template.Template;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.iotdb.session.Session;
import org.apache.iotdb.session.template.MeasurementNode;
import org.apache.tsfile.enums.TSDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.nomelin.iot.common.annotation.LogExecutionTime;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.SystemException;
import top.nomelin.iot.dao.IoTDBDao;
import top.nomelin.iot.model.dto.DeviceTable;
import top.nomelin.iot.model.dto.Record;
import top.nomelin.iot.util.SessionContext;
import top.nomelin.iot.util.TimeUtil;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class IoTDBDaoImpl implements IoTDBDao {
    private static final Logger log = LoggerFactory.getLogger(IoTDBDaoImpl.class);
    @Value("${iotdb.query.timeout}")
    long queryTimeout;

    private Session getSession() {
        return SessionContext.getCurrentSession();
    }

    @Override
    public void createDatabase(String databasePath) {
        try {
            getSession().createDatabase(databasePath);
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
        log.info("创建数据库 {} 成功", databasePath);
    }

    @Override
    public void createSchema(String schemaName, List<MeasurementNode> measurementNodes) {
        Template template = new Template(schemaName, true);
        try {
            for (MeasurementNode measurementNode : measurementNodes) {
                template.addToTemplate(measurementNode);
            }
            getSession().createSchemaTemplate(template);
        } catch (StatementExecutionException | IoTDBConnectionException | IOException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
        log.info("创建 元数据模板 {} 成功", schemaName);
    }

    @Override
    public void setAndActivateSchema(String schemaName, String devicePath) {
        try {
            List<String> paths = queryPathsSchemaSetOn(schemaName);
            boolean isSchemaSetOn = false;
            if (paths.contains(devicePath)) {
                log.info("元数据模板 {} 已经挂载到路径 {} 了", schemaName, devicePath);
                isSchemaSetOn = true;
            }
            if (!isSchemaSetOn) {
                getSession().setSchemaTemplate(schemaName, devicePath);
                log.info("挂载 元数据模板 {} 到路径 {} 成功", schemaName, devicePath);
            }
            boolean isSchemaUsingOn = false;
            paths = queryPathsSchemaUsingOn(schemaName);
            if (paths.contains(devicePath)) {
                log.info("元数据模板 {} 已经在设备 {} 上激活", schemaName, devicePath);
                isSchemaUsingOn = true;
            }
            if (!isSchemaUsingOn) {
                executeNonQueryStatement("CREATE TIMESERIES OF SCHEMA TEMPLATE on " + devicePath);
                log.info("激活 元数据模板 {} 到设备 {} 成功, 并创建时间序列", schemaName, devicePath);
            }
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
    }

    @Override
    public void deActiveAndUnsetSchema(String schemaName, String devicePath) {
        try {
            // 检查模板是否在该设备路径上被激活
            boolean isSchemaUsingOn = false;
            List<String> usingPaths = queryPathsSchemaUsingOn(schemaName);
            if (usingPaths.contains(devicePath)) {
                String deactivateSql = "DEACTIVATE DEVICE TEMPLATE " + schemaName + " FROM " + devicePath;
                executeNonQueryStatement(deactivateSql);
                log.info("解除模板 {} 在设备 {} 的激活状态，并删除时间序列", schemaName, devicePath);
                isSchemaUsingOn = true;
            }
            if (!isSchemaUsingOn) {
                log.info("模板 {} 不在设备 {} 上激活，无需解除激活", schemaName, devicePath);
            }

            // 检查模板是否挂载到该设备路径
            boolean isSchemaSetOn = false;
            List<String> setPaths = queryPathsSchemaSetOn(schemaName);
            if (setPaths.contains(devicePath)) {
                getSession().unsetSchemaTemplate(devicePath, schemaName);
                log.info("卸载模板 {} 在设备 {} 的挂载", schemaName, devicePath);
                isSchemaSetOn = true;
            }
            if (!isSchemaSetOn) {
                log.info("模板 {} 不在设备 {} 上挂载，无需卸载", schemaName, devicePath);
            }

        } catch (Exception e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
    }

    @Override
    public void deleteSchema(String schemaName) {
        try {
            // 处理所有使用该模板的路径
            List<String> usingPaths = queryPathsSchemaUsingOn(schemaName);
            for (String path : usingPaths) {
                String deactivateSql = "DEACTIVATE DEVICE TEMPLATE " + schemaName + " FROM " + path;
                executeNonQueryStatement(deactivateSql);
                log.info("解除模板 {} 在设备 {} 的激活状态，并删除时间序列", schemaName, path);
            }

            // 处理所有挂载该模板的路径
            List<String> setPaths = queryPathsSchemaSetOn(schemaName);
            for (String path : setPaths) {
                getSession().unsetSchemaTemplate(path, schemaName);
                log.info("卸载模板 {} 在设备 {} 的挂载", schemaName, path);
            }

            // 删除模板
            String dropSql = "DROP DEVICE TEMPLATE " + schemaName;
            executeNonQueryStatement(dropSql);
            log.info("删除模板 {} 成功", schemaName);
        } catch (Exception e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }

    }


    @Override
    public List<String> queryAllSchemas() {
        List<String> schemaNames;
        try {
            schemaNames = getSession().showAllTemplates();
        } catch (StatementExecutionException | IoTDBConnectionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
        log.info("查询元数据模板列表成功, 共 {} 个模板", schemaNames.size());
        return schemaNames;
    }

    @Override
    public List<String> queryPathsSchemaSetOn(String schemaName) {
        List<String> paths;
        try {
            paths = getSession().showPathsTemplateSetOn(schemaName);
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
        log.info("查询元数据模板 {} 被挂载的路径列表成功, 共 {} 个路径", schemaName, paths.size());
        return paths;
    }

    @Override
    public List<String> queryPathsSchemaUsingOn(String schemaName) {
        List<String> paths;
        try {
            paths = getSession().showPathsTemplateUsingOn(schemaName);
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
        log.info("查询元数据模板 {} 被使用到的路径列表成功, 共 {} 个路径", schemaName, paths.size());
        return paths;
    }

    @Override
    public List<String> showMeasurementsInSchema(String schemaName) {
        List<String> measurements;
        try {
            measurements = getSession().showMeasurementsInTemplate(schemaName);
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
        log.info("查询元数据模板 {} 中的物理量列表成功, 共 {} 个物理量", schemaName, measurements.size());
        return measurements;
    }

    @LogExecutionTime
    @Override
    public void insertAlignedRecord(String devicePath, long time, List<String> measurements, List<TSDataType> types, List<Object> values) {
        try {
            getSession().insertAlignedRecord(devicePath, time, measurements, types, values);
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
        log.info("向 {} 插入一条对齐记录成功，时间戳为 {}", devicePath, time);
    }

    @LogExecutionTime
    @Override
    public void insertBatchAlignedRecordsOfOneDevice(String devicePath, List<Long> times, List<List<String>> measurementsList, List<List<TSDataType>> typesList, List<List<Object>> valuesList) {
        try {
            getSession().insertAlignedRecordsOfOneDevice(devicePath, times, measurementsList, typesList, valuesList);
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
        log.info("向 {} 插入 {} 条对齐记录成功", devicePath, times.size());
    }

    @LogExecutionTime
    @Override
    public DeviceTable queryRecords(String devicePath, long startTime, long endTime) {
        List<String> paths = List.of(devicePath + ".*");//devicePath.*
        return queryRecordsByPaths(devicePath, startTime, endTime, paths);
    }

    @LogExecutionTime
    @Override
    public DeviceTable queryRecords(String devicePath, long startTime, long endTime, List<String> selectFields) {
        //构造查询路径列表。device.measurement1, device.measurement2, ...
        List<String> paths = new ArrayList<>();
        for (String measurement : selectFields) {
            paths.add(devicePath + "." + measurement);
        }
        return queryRecordsByPaths(devicePath, startTime, endTime, paths);
    }

    @Override
    public void deleteDatabase(String databasePath) {
        try {
            getSession().deleteDatabase(databasePath);
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
        log.info("删除数据库 {} 成功", databasePath);
    }

    @Override
    public Map<String, String> getExistingMeasurements(String devicePath, long timestamp) {
        // 构造查询路径，查询设备所有测量（例如：devicePath.*）
        List<String> paths = List.of(devicePath + ".*");
        // 使用 queryRecordsByPaths 方法查询 [timestamp, timestamp+1) 的数据，确保只获取该时刻的数据记录
        DeviceTable deviceTable = queryRecordsByPaths(devicePath, timestamp, timestamp + 1, paths);

        Map<String, String> existingData = new HashMap<>();
        // 从 DeviceTable 中获取指定 timestamp 对应的记录列表
        List<Record> recordsAtTimestamp = deviceTable.getRecords().get(timestamp);
        if (recordsAtTimestamp != null && !recordsAtTimestamp.isEmpty()) {
            // 假设该时间戳下只有一条记录，取第一条
            Record record = recordsAtTimestamp.get(0);
            // 遍历记录中的所有字段，将每个测量名称和其对应的值转换为字符串存入返回的 Map 中
            for (Map.Entry<String, Object> entry : record.getFields().entrySet()) {
                // 如果值为 null，也可以根据业务需要处理为默认值
                existingData.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
            }
        } else {
            log.info("设备 {} 在时间戳 {} 没有查询到数据", devicePath, timestamp);
        }
        log.info("查询设备 {} 时间戳 {} 的数据成功, result: {}", devicePath, timestamp, existingData);
        return existingData;
    }

    @Override
    public Map<Long, Map<String, String>> getExistingMeasurementsBatch(String devicePath, List<Long> windowTimestamps) {
        if (windowTimestamps.isEmpty()) {
            return Collections.emptyMap();
        }

        // 构造时间范围查询条件：timestamp >= windowTs AND timestamp < windowTs+1
        String timeFilter = windowTimestamps.stream()
                .map(ts -> String.format("(time = %d)", ts))
                .collect(Collectors.joining(" OR "));

        String sql = String.format("SELECT * FROM %s WHERE %s", devicePath, timeFilter);
        SessionDataSet sessionDataSet = executeQueryStatement(sql);
        DeviceTable deviceTable;
        try {
            deviceTable = DeviceTable.convertToDeviceTable(sessionDataSet, devicePath);
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
        Map<Long, Map<String, String>> existingData = new HashMap<>();
        for (Long windowTs : windowTimestamps) {
            List<Record> recordsAtTimestamp = deviceTable.getRecords().get(windowTs);
            if (recordsAtTimestamp != null && !recordsAtTimestamp.isEmpty()) {
                // 假设该时间戳下只有一条记录，取第一条
                Record record = recordsAtTimestamp.get(0);
                Map<String, String> dataAtTimestamp = new HashMap<>();
                for (Map.Entry<String, Object> entry : record.getFields().entrySet()) {
                    // 如果值为 null，也可以根据业务需要处理为默认值
                    dataAtTimestamp.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : null);
                }
                existingData.put(windowTs, dataAtTimestamp);
            } else {
//                log.info("[batch]设备 {} 在时间戳 {} 没有查询到数据", devicePath, windowTs);
            }
        }
        log.info("[batch]查询设备 {} 时间戳 {} 的数据成功, result: {}", devicePath, windowTimestamps, existingData);
        return existingData;

    }


    private DeviceTable queryRecordsByPaths(String devicePath, long startTime, long endTime, List<String> paths) {
        SessionDataSet sessionDataSet;
        DeviceTable deviceTable;
        try {
            sessionDataSet = getSession().executeRawDataQuery(paths, startTime, endTime, queryTimeout);
            deviceTable = DeviceTable.convertToDeviceTable(sessionDataSet, devicePath);
        } catch (StatementExecutionException | IoTDBConnectionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
        log.info("查询设备 {} , 时间范围 {} 至 {} 的数据成功, 共 {} 条记录",
                devicePath, TimeUtil.timestampToDateString(startTime), TimeUtil.timestampToDateString(endTime),
                deviceTable.getRecords().size());
        return deviceTable;
    }

    @LogExecutionTime
    private void executeNonQueryStatement(String sql) {
        try {
            getSession().executeNonQueryStatement(sql);
            log.info("执行非查询 SQL 语句 {} 成功", sql);
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, "SQL: " + sql, e);
        }
    }

    @LogExecutionTime
    private SessionDataSet executeQueryStatement(String sql) {
        SessionDataSet sessionDataSet;
        try {
            sessionDataSet = getSession().executeQueryStatement(sql);
            log.info("执行查询 SQL 语句 {} 成功", sql);
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, "SQL: " + sql, e);
        }
        return sessionDataSet;
    }
}
