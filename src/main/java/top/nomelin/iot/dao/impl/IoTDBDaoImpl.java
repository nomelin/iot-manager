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
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.SystemException;
import top.nomelin.iot.dao.IoTDBDao;
import top.nomelin.iot.model.DeviceTable;
import top.nomelin.iot.util.TimeUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class IoTDBDaoImpl implements IoTDBDao {
    private static final Logger log = LoggerFactory.getLogger(IoTDBDaoImpl.class);
    private final Session session;
    /*    @Value("${iotdb.storage.ts_encoding}")
        String iotdbStorageTsEncoding;
        @Value("${iotdb.storage.compression_type}")
        String iotdbStorageCompressionType;*/
    @Value("${iotdb.query.timeout}")
    long queryTimeout;

    public IoTDBDaoImpl(Session session) {
        this.session = session;
    }

    //TODO aop实现方法执行前开启session，方法执行后关闭session。还可以实现这两个异常的统一处理。
    @Override
    public void createDatabase(String databasePath) {
        try {
            session.createDatabase(databasePath);
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
            session.createSchemaTemplate(template);
        } catch (StatementExecutionException | IoTDBConnectionException | IOException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
        log.info("创建 元数据模板 {} 成功", schemaName);
    }

    @Override
    public void setAndActivateSchema(String schemaName, String databaseName, String deviceName) {
        try {
            session.setSchemaTemplate(schemaName, databaseName + "." + deviceName);
            log.info("挂载 元数据模板 {} 到路径 {}.{} 成功", schemaName, databaseName, deviceName);
            executeNonQueryStatement("CREATE TIMESERIES OF SCHEMA TEMPLATE on " + databaseName + "." + deviceName);
            log.info("激活 元数据模板 {} 到设备 {}.{} 成功, 并创建时间序列", schemaName, databaseName, deviceName);
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
    }

    @Override
    public List<String> queryAllSchemas() {
        List<String> schemaNames;
        try {
            schemaNames = session.showAllTemplates();
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
            paths = session.showPathsTemplateSetOn(schemaName);
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
            paths = session.showPathsTemplateUsingOn(schemaName);
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
            measurements = session.showMeasurementsInTemplate(schemaName);
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
        log.info("查询元数据模板 {} 中的物理量列表成功, 共 {} 个物理量", schemaName, measurements.size());
        return measurements;
    }

    @Override
    public void insertAlignedRecord(String devicePath, long time, List<String> measurements, List<TSDataType> types, List<Object> values) {
        try {
            session.insertAlignedRecord(devicePath, time, measurements, types, values);
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
        log.info("向 {} 插入一条对齐记录成功，时间戳为 {}", devicePath, time);
    }

    @Override
    public void insertBatchAlignedRecordsOfOneDevice(String devicePath, List<Long> times, List<List<String>> measurementsList, List<List<TSDataType>> typesList, List<List<Object>> valuesList) {
        try {
            session.insertAlignedRecordsOfOneDevice(devicePath, times, measurementsList, typesList, valuesList);
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
        log.info("向 {} 插入 {} 条对齐记录成功", devicePath, times.size());
    }

    @Override
    public DeviceTable queryRecords(String devicePath, long startTime, long endTime) {
        List<String> paths = List.of(devicePath + ".*");//devicePath.*
        return queryRecordsByPaths(devicePath, startTime, endTime, paths);
    }


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
            session.deleteDatabase(databasePath);
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
        log.info("删除数据库 {} 成功", databasePath);
    }


    private DeviceTable queryRecordsByPaths(String devicePath, long startTime, long endTime, List<String> paths) {
        SessionDataSet sessionDataSet;
        DeviceTable deviceTable;
        try {
            sessionDataSet = session.executeRawDataQuery(paths, startTime, endTime, queryTimeout);
            deviceTable = DeviceTable.convertToDeviceTable(sessionDataSet, devicePath);
        } catch (StatementExecutionException | IoTDBConnectionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
        log.info("查询设备 {} , 时间范围 {} 至 {} 的数据成功, 共 {} 条记录",
                devicePath, TimeUtil.timestampToDateString(startTime), TimeUtil.timestampToDateString(endTime),
                deviceTable.getRecords().size());
        return deviceTable;
    }

    private void executeNonQueryStatement(String sql) {
        try {
            session.executeNonQueryStatement(sql);
            log.info("执行非查询 SQL 语句 {} 成功", sql);
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
    }

    private SessionDataSet executeQueryStatement(String sql) {
        SessionDataSet sessionDataSet;
        try {
            sessionDataSet = session.executeQueryStatement(sql);
            log.info("执行查询 SQL 语句 {} 成功", sql);
        } catch (IoTDBConnectionException | StatementExecutionException e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
        return sessionDataSet;
    }
}
