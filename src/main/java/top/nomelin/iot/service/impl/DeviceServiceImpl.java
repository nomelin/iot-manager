package top.nomelin.iot.service.impl;

import cn.hutool.core.util.ObjectUtil;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import top.nomelin.iot.cache.CurrentUserCache;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.dao.DeviceMapper;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.service.DeviceService;

import java.util.ArrayList;
import java.util.List;

/**
 * DeviceServiceImpl
 *
 * @author nomelin
 * @since 2024/12/21 22:24
 **/
@Service
public class DeviceServiceImpl implements DeviceService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DeviceServiceImpl.class);
    private final DeviceMapper deviceMapper;
    private final CurrentUserCache currentUserCache;

    public DeviceServiceImpl(DeviceMapper deviceMapper, CurrentUserCache currentUserCache) {
        this.deviceMapper = deviceMapper;
        this.currentUserCache = currentUserCache;
    }

    /**
     * 检查设备权限,同时返回设备信息
     */
    @Override
    public Device checkPermission(int deviceId) {
        Device device = deviceMapper.selectById(deviceId);
        if (ObjectUtil.isNull(device)) {
            log.warn("设备不存在, deviceId: {}", deviceId);
            throw new BusinessException(CodeMessage.NOT_FOUND_ERROR);
        }
        if (!device.getUserId().equals(currentUserCache.getCurrentUser().getId())) {
            log.warn("非法操作, 非设备拥有者操作设备 deviceId: {}, userId: {}", deviceId, device.getUserId());
            throw new BusinessException(CodeMessage.NO_PERMISSION_ERROR);
        }
        return device;
    }

    @Override
    public Device addDevice(Device device) {
        device.setUserId(currentUserCache.getCurrentUser().getId());
        deviceMapper.insert(device);
        log.info("添加设备成功, device: {}", device);
        return device;
    }

    @Override
    public void deleteDevice(int deviceId) {
        checkPermission(deviceId);
        deviceMapper.delete(deviceId);
        log.info("删除设备成功, deviceId: {}", deviceId);
    }

    @Override
    public void updateDeviceName(int deviceId, String name) {
        checkPermission(deviceId);
        Device device = new Device();
        device.setId(deviceId);
        device.setName(name);
        deviceMapper.update(device);
        log.info("更新设备名称成功, deviceId: {}, name: {}", deviceId, name);
    }

    @Override
    public Device addTags(int deviceId, List<String> tags) {
        //获取已有tags，并校验是否有重复的tag。重复的tag不添加
        Device device = checkPermission(deviceId);
        List<String> oldTags = device.getTags();
        if (ObjectUtil.isNull(oldTags)) {
            oldTags = new ArrayList<>();
        }
        for (String tag : tags) {
            if (oldTags.contains(tag)) {
                log.warn("设备 {} 已有标签 {}", deviceId, tag);
                continue;
            }
            oldTags.add(tag);
        }
        device.setTags(oldTags);
        deviceMapper.update(device);
        log.info("添加设备标签成功, deviceId: {}, tags: {}", deviceId, tags);
        return device;
    }

    @Override
    public Device removeTags(int deviceId, List<String> tags) {
        Device device = checkPermission(deviceId);
        List<String> oldTags = device.getTags();
        if (ObjectUtil.isNull(oldTags)) {
            oldTags = new ArrayList<>();
        }
        for (String tag : tags) {
            if (!oldTags.contains(tag)) {
                log.warn("设备 {} 没有标签 {}", deviceId, tag);
                continue;
            }
            oldTags.remove(tag);
        }
        device.setTags(oldTags);
        deviceMapper.update(device);
        log.info("删除设备标签成功, deviceId: {}, tags: {}", deviceId, tags);
        return device;
    }

    @Override
    public Device getDeviceById(int deviceId) {
        return checkPermission(deviceId);
    }

    @Override
    public List<Device> getAllDevice() {
        Device device = new Device();
        device.setUserId(currentUserCache.getCurrentUser().getId());
        return deviceMapper.selectAll(device);
    }
}
