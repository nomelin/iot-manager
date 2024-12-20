package top.nomelin.iot.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.nomelin.iot.common.Result;
import top.nomelin.iot.model.DeviceTable;
import top.nomelin.iot.model.Record;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * DataController
 *
 * @author nomelin
 * @since 2024/12/18 21:30
 **/
@RestController
@RequestMapping("/data")
public class DataController {
    @RequestMapping("/test/{deviceId}/{extraDataNum}")
    public Result test(@PathVariable("deviceId") String deviceId, @PathVariable("extraDataNum") int extraDataNum) {
        // 配置生成数据的参数
        long startTimestamp = 1734529151000L; // 起始时间戳
        int durationSeconds = 100; // 持续时间（秒）
        int intervalMillis = 1000; // 时间间隔（毫秒）
        double tempMin = -10.0, tempMax = 30.0; // 温度范围
        int humidityMin = 20, humidityMax = 60; // 湿度范围

        // 额外数据
        int extraDataMin = 0, extraDataMax = 100; // 额外数据范围

        // 创建 DeviceTable 对象
        DeviceTable deviceTable = new DeviceTable();
        deviceTable.setDevicePath("root.123.testdevice" + deviceId);
        deviceTable.setTypes(Arrays.asList("DOUBLE", "INT32"));

        Map<Long, Record> records = new LinkedHashMap<>();
        Random random = new Random();

        for (int i = 0; i < durationSeconds; i++) {
            long timestamp = startTimestamp + i * intervalMillis;

            // 创建 Record 对象并填充字段数据
            Record record = new Record();
            record.getFields().put("temperature", tempMin + (tempMax - tempMin) * random.nextDouble());
            record.getFields().put("humidity", humidityMin + random.nextInt(humidityMax - humidityMin + 1));
            for (int j = 0; j < extraDataNum; j++) {
                record.getFields().put("extraData" + j, extraDataMin + random.nextInt(extraDataMax - extraDataMin + 1));
            }
            records.put(timestamp, record);
        }

        deviceTable.setRecords(records);

        // 返回数据
        return Result.success(deviceTable);
    }

}
