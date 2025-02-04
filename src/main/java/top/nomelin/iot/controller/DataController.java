package top.nomelin.iot.controller;

import cn.hutool.core.util.ObjectUtil;
import org.springframework.web.bind.annotation.*;
import top.nomelin.iot.common.Result;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.controller.request.DataInsertRequest;
import top.nomelin.iot.controller.request.DataQueryRequest;
import top.nomelin.iot.model.dto.DeviceTable;
import top.nomelin.iot.model.dto.Record;
import top.nomelin.iot.service.DataService;

import java.util.*;

/**
 * DataController
 *
 * @author nomelin
 */
@RestController
@RequestMapping("/data")
public class DataController {
    private final DataService dataService;

    public DataController(DataService dataService) {
        this.dataService = dataService;
    }

    @PostMapping("/insert")
    public Result insertBatchData(@RequestBody DataInsertRequest request) {
        if (request.getDeviceId() == null || ObjectUtil.isEmpty(request.getTimestamps()) ||
                ObjectUtil.isEmpty(request.getMeasurements()) || ObjectUtil.isEmpty(request.getValues())) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR, "参数缺失,request:" + request);
        }
        if (request.getTimestamps().size() != request.getValues().size()) {
            throw new BusinessException(CodeMessage.INSERT_VALUE_NUM_ERROR,
                    "时间戳数量：" + request.getTimestamps().size() + "，数据行数量：" + request.getValues().size());
        }
        if (request.getMeasurements().size() != request.getValues().get(0).size()) {
            throw new BusinessException(CodeMessage.INSERT_MEASUREMENT_NUM_ERROR,
                    "属性数量：" + request.getMeasurements().size() + "，数据列数量：" + request.getValues().get(0).size());
        }
        dataService.insertBatchRecord(request.getDeviceId(), request.getTimestamps(),
                request.getMeasurements(), request.getValues());
        return Result.success();
    }

    @PostMapping("/query")
    public Result queryData(@RequestBody DataQueryRequest request) {
        if (request.getDeviceId() == null || request.getStartTime() == null || request.getEndTime() == null) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR, "参数缺失,request:" + request);
        }
        if (request.getStartTime() > request.getEndTime()) {
            throw new BusinessException(CodeMessage.PARAM_ERROR, "开始时间不能大于结束时间");
        }
        DeviceTable result = dataService.queryRecord(
                request.getDeviceId(),
                request.getStartTime(),
                request.getEndTime(),
                request.getSelectMeasurements(),
                request.getAggregationTime(),
                request.getQueryAggregateFunc(),
                request.getThresholds()
        );
        return Result.success(result);
    }
    //TODO 同一个时间戳多个值，在前端怎么处理？

    @RequestMapping("/test/{deviceId}/{extraDataNum}")
    public Result test(@PathVariable("deviceId") String deviceId, @PathVariable("extraDataNum") int extraDataNum) {
        // 配置生成数据的参数
        long startTimestamp = 1734529151000L; // 起始时间戳
        int durationSeconds = 10; // 持续时间（秒）
        int intervalMillis = 1000; // 时间间隔（毫秒）
        double tempMin = -10.0, tempMax = 30.0; // 温度范围
        int humidityMin = 20, humidityMax = 60; // 湿度范围

        // 额外数据
        int extraDataMin = 0, extraDataMax = 100; // 额外数据范围

        // 创建 DeviceTable 对象
        DeviceTable deviceTable = new DeviceTable();
        deviceTable.setDevicePath("root.123.testdevice" + deviceId);
        deviceTable.setTypes(Arrays.asList("DOUBLE", "INT32"));

        Map<Long, List<Record>> records = new LinkedHashMap<>();
        Random random = new Random();

        // 随机选择几个时间戳
        Set<Long> selectedTimestamps = new HashSet<>();
        while (selectedTimestamps.size() < 3) {  // 假设我们随机选择3个时间戳
            long timestamp = startTimestamp + random.nextInt(durationSeconds) * intervalMillis;
            selectedTimestamps.add(timestamp);
        }

        // 为每个选中的时间戳生成多个数据行
        for (long timestamp : selectedTimestamps) {
            int dataRowsCount = 1 + random.nextInt(3);  // 每个时间戳随机生成1-3个数据行
            List<Record> recordList = new ArrayList<>();

            for (int i = 0; i < dataRowsCount; i++) {
                Record record = new Record();
                // 小概率给null值
                if (random.nextInt(100) < 0) {
                    record.getFields().put("temperature", null);
                    record.getFields().put("humidity", null);
                } else {
                    record.getFields().put("temperature", tempMin + (tempMax - tempMin) * random.nextDouble());
                    record.getFields().put("humidity", humidityMin + random.nextInt(humidityMax - humidityMin + 1));
                    for (int j = 0; j < extraDataNum; j++) {
                        record.getFields().put("extraData" + j, extraDataMin + random.nextInt(extraDataMax - extraDataMin + 1));
                    }
                }
                recordList.add(record);
            }

            records.put(timestamp, recordList);
        }

        deviceTable.setRecords(records);

        // 返回数据
        return Result.success(deviceTable);
    }


}