package top.nomelin.iot.dao;

import org.apache.tsfile.enums.TSDataType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.nomelin.iot.model.dto.DeviceTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
class IoTDBDaoTest {

    @Autowired
    private IoTDBDao dao;

    @Test
    void test() {
        // 1. 创建数据库 root.test
        String databasePath = "root.test1";
//        dao.createDatabase(databasePath);
//        System.out.println("Database 'root.test' created successfully.");

        // 2. 创建元数据模板
        String schemaName = "testSchema1";

//        MeasurementNode temperatureNode = new MeasurementNode("temperature1", TSDataType.FLOAT,
//                TSEncoding.PLAIN, CompressionType.UNCOMPRESSED);
//        MeasurementNode humidityNode = new MeasurementNode("humidity2", TSDataType.INT32, TSEncoding.PLAIN,
//                CompressionType.UNCOMPRESSED);
//        List<MeasurementNode> measurementNodes = Arrays.asList(temperatureNode, humidityNode);
//
//        dao.createSchema(schemaName, measurementNodes);
        System.out.println("Schema 'testSchema' created successfully.");

        // 3. 挂载并激活元数据模板到设备
        String devicePath = "root.test1.testdevice1";
        dao.setAndActivateSchema(schemaName, devicePath);
        System.out.println("Schema 'testSchema' activated on device 'root.test.testdevice'.");

        //4. 插入一行数据
        long timestamp = System.currentTimeMillis();
        List<String> measurements = Arrays.asList("temperature1", "humidity2");
        List<TSDataType> types = Arrays.asList(TSDataType.FLOAT, TSDataType.INT32);
        List<Object> values = Arrays.asList(23.5f, 80);

        dao.insertAlignedRecord(devicePath, timestamp, measurements, types, values);
        System.out.println("Inserted a record with timestamp " + timestamp);

        // 5. 查询插入的数据
        long startTime = 1734269655847L;  // 查询范围的起始时间
        long endTime = 1734270790207L;    // 查询范围的结束时间
        DeviceTable deviceTable = dao.queryRecords(devicePath, startTime, endTime);
        System.out.println("Query results: " + formatJson(deviceTable));

/*        // 6. 查询所有模板
        List<String> schemas = dao.queryAllSchemas();
        System.out.println("All schemas: " + schemas);

        // 7. 查询某模板被设置到 MTree 的所有路径
        List<String> pathsSetOnSchema = dao.queryPathsSchemaSetOn(schemaName);
        System.out.println("Paths where schema '" + schemaName + "' is set: " + pathsSetOnSchema);

        // 8. 查询所有使用某模板的路径
        List<String> pathsUsingSchema = dao.queryPathsSchemaUsingOn(schemaName);
        System.out.println("Paths using schema '" + schemaName + "': " + pathsUsingSchema);

        // 9. 显示模板中所有的物理量路径
        List<String> measurementsInSchema = dao.showMeasurementsInSchema(schemaName);
        System.out.println("Measurements in schema '" + schemaName + "': " + measurementsInSchema);*/

        // 10. 批量插入多行数据
//        List<Long> timestamps = Arrays.asList(System.currentTimeMillis(), System.currentTimeMillis() + 1000);
//        List<List<String>> measurementsList = Arrays.asList(
//                Arrays.asList("temperature", "humidity"),
//                Arrays.asList("temperature", "humidity")
//        );
//        List<List<TSDataType>> typesList = Arrays.asList(
//                Arrays.asList(TSDataType.FLOAT, TSDataType.INT32),
//                Arrays.asList(TSDataType.FLOAT, TSDataType.INT32)
//        );
//        List<List<Object>> valuesList = Arrays.asList(
//                Arrays.asList(24.5f, 65),
//                Arrays.asList(25.5f, 70)
//        );
//
//        dao.insertBatchAlignedRecordsOfOneDevice(devicePath, timestamps, measurementsList, typesList, valuesList);
//        System.out.println("Inserted batch records for device: " + devicePath);

        // 11. 按字段查询
        List<String> selectFields = List.of("temperature");
        DeviceTable fieldQueryResults = dao.queryRecords(devicePath, startTime, endTime, selectFields);
        System.out.println("Query results (selecting fields): " + formatJson(fieldQueryResults));

/*        // 12. 删除数据库 root.test
        dao.deleteDatabase(databasePath);
        System.out.println("Database 'root.test' deleted successfully.");*/
    }

    @Test
    void testDelete() {
        String device = "root.data.user_1.device_43";
        dao.deleteData(device, 1719093773000L, 1719093776000L);
        List<Long> timestamps = new ArrayList<>();
        timestamps.add(1719093778000L);
        timestamps.add(1719093779000L);
        timestamps.add(1719093780000L);
        timestamps.add(1719093782000L);
        timestamps.add(1719093784000L);
        timestamps.add(1719093785000L);
        dao.deleteData(device, timestamps);
    }

    @Test
    void testQueryCount() {
        String device = "root.data.user_1.device_61";
        long startTime = 1719093771000L;
        long endTime = 1742041610000L;
        long count = dao.queryRecordsCount(device, startTime, endTime);
        System.out.println("Count: " + count);
        long count2 = dao.queryRecordsCount(device, null, null);
        System.out.println("Count: " + count2);
    }

    // 格式化查询结果为 JSON 字符串
    private String formatJson(DeviceTable deviceTable) {
        // 这里我们需要将 deviceTable 转换为 JSON 格式
        // 假设 deviceTable 有一个 toJson 方法，或者你可以使用 Jackson 或 Gson 库来实现转换

        // 使用 Jackson 格式化为 JSON 字符串
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.writeValueAsString(deviceTable);
        } catch (Exception e) {
            e.printStackTrace();
            return "{}"; // 如果失败返回一个空 JSON 对象
        }
    }
}
