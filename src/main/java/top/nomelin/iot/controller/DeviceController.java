package top.nomelin.iot.controller;

import cn.hutool.core.util.ObjectUtil;
import org.springframework.web.bind.annotation.*;
import top.nomelin.iot.common.Result;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.service.DeviceService;

import java.util.List;

/**
 * DeviceController
 *
 * @author nomelin
 * @since 2024/12/21 22:45
 **/
@RestController
@RequestMapping("/device")
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @RequestMapping("/add")
    public Result addDevice(@RequestBody Device device) {
        if (ObjectUtil.isNull(device) || ObjectUtil.isEmpty(device.getName())) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        return Result.success(deviceService.addDevice(device));
    }

    @RequestMapping("/delete/{deviceId}")
    public Result deleteDevice(@PathVariable Integer deviceId) {
        if (ObjectUtil.isNull(deviceId)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        deviceService.deleteDevice(deviceId);
        return Result.success();
    }

    @RequestMapping("/updateName/{deviceId}")
    public Result updateDeviceName(@PathVariable Integer deviceId, @RequestParam String name) {
        if (ObjectUtil.isNull(deviceId) || ObjectUtil.isEmpty(name)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        deviceService.updateDeviceName(deviceId, name);
        return Result.success();
    }

    @RequestMapping("/addTags/{deviceId}")
    public Result addTags(@PathVariable Integer deviceId, @RequestBody List<String> tags) {
        if (ObjectUtil.isNull(deviceId) || ObjectUtil.isEmpty(tags)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        return Result.success(deviceService.addTags(deviceId, tags));
    }

    @RequestMapping("/removeTags/{deviceId}")
    public Result removeTags(@PathVariable Integer deviceId, @RequestBody List<String> tags) {
        if (ObjectUtil.isNull(deviceId) || ObjectUtil.isEmpty(tags)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        return Result.success(deviceService.removeTags(deviceId, tags));
    }

    @RequestMapping("/get/{deviceId}")
    public Result getDeviceById(@PathVariable Integer deviceId) {
        if (ObjectUtil.isNull(deviceId)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        return Result.success(deviceService.getDeviceById(deviceId));
    }

    @RequestMapping("/all")
    public Result getAllDevice() {
        return Result.success(deviceService.getAllDevice());
    }
}
