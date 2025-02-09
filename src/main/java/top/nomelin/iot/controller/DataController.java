package top.nomelin.iot.controller;

import cn.hutool.core.util.ObjectUtil;
import org.springframework.web.bind.annotation.*;
import top.nomelin.iot.common.Result;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.controller.request.DataInsertRequest;
import top.nomelin.iot.controller.request.DataQueryRequest;
import top.nomelin.iot.controller.response.EnumInfo;
import top.nomelin.iot.model.dto.DeviceTable;
import top.nomelin.iot.model.enums.QueryAggregateFunc;
import top.nomelin.iot.model.enums.StorageMode;
import top.nomelin.iot.service.DataService;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * 获取所有存储模式
     */
    @GetMapping("/storageModes")
    public Result getStorageModes() {
        List<EnumInfo> modes = new ArrayList<>();
        for (StorageMode mode : StorageMode.values()) {
            modes.add(new EnumInfo(
                    mode.name(),
                    mode.getName(),
                    mode.getDesc()
            ));
        }
        return Result.success(modes);
    }

    /**
     * 获取所有聚合函数
     */
    @GetMapping("/aggregateFuncs")
    public Result getAggregateFunctions() {
        List<EnumInfo> funcs = new ArrayList<>();
        for (QueryAggregateFunc func : QueryAggregateFunc.values()) {
            funcs.add(new EnumInfo(
                    func.name(),
                    func.getName(),
                    func.getDesc()
            ));
        }
        return Result.success(funcs);
    }

}