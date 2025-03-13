package top.nomelin.iot.service.impl;

import cn.hutool.core.util.ObjectUtil;
import org.slf4j.Logger;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.nomelin.iot.cache.CurrentUserCache;
import top.nomelin.iot.common.Constants;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.common.exception.SystemException;
import top.nomelin.iot.dao.DeviceMapper;
import top.nomelin.iot.dao.IoTDBDao;
import top.nomelin.iot.model.Config;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.Group;
import top.nomelin.iot.model.Template;
import top.nomelin.iot.service.DeviceService;
import top.nomelin.iot.service.GroupService;
import top.nomelin.iot.service.TemplateService;
import top.nomelin.iot.service.storage.StorageStrategy;
import top.nomelin.iot.service.storage.StorageStrategyManager;
import top.nomelin.iot.util.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final TemplateService templateService;
    private final CurrentUserCache currentUserCache;
    private final StorageStrategyManager storageStrategyManager;
    private final GroupService groupService;//懒加载，避免循环依赖

    private final IoTDBDao iotDBDao;

    public DeviceServiceImpl(DeviceMapper deviceMapper, TemplateService templateService, CurrentUserCache currentUserCache,
                             StorageStrategyManager storageStrategyManager, @Lazy GroupService groupService, IoTDBDao iotDBDao) {
        this.deviceMapper = deviceMapper;
        this.templateService = templateService;
        this.currentUserCache = currentUserCache;
        this.storageStrategyManager = storageStrategyManager;
        this.groupService = groupService;
        this.iotDBDao = iotDBDao;
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
    public Device addDevice(Device device, int templateId) {
        device.setUserId(currentUserCache.getCurrentUser().getId());
        device.setTemplateId(templateId);
        //使用模板的配置作为设备的基本配置
        Template template = templateService.getTemplateById(templateId);
        Config config = mergeConfig(template, device);//合并模板和设备的配置
        device.setConfig(config);
        StorageStrategy storageStrategy = storageStrategyManager.getStrategy(config.getStorageMode());
        //插入mysql。因为后面要使用id，所以必须先插入mysql。
        deviceMapper.insert(device);
        log.info("添加设备到mysql成功, device: {}", device);
        //创建iotdb设备。实际上databasePath不是数据库。
        //使用存储策略对应的模板
        iotDBDao.setAndActivateSchema(
                Constants.TEMPLATE_PREFIX + templateId + storageStrategy.getTemplateSuffix(),
                util.getDevicePath(device.getUserId(), device.getId()));
        log.info("创建iotdb设备成功, templateName: {}, database: {}",
                Constants.TEMPLATE_PREFIX + templateId + storageStrategy.getTemplateSuffix(),
                Constants.DATABASE_PREFIX + device.getUserId());
        return device;
    }

    //把模板的配置和接口传入的设备配置合并。只处理部分配置项。
    //目前只处理storageMode。
    private Config mergeConfig(Template template, Device device) {
        if (ObjectUtil.isNull(template.getConfig())) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR, "模板config为空,模板id:" + template.getId());
        }
        Config newConfig = template.getConfig();
        newConfig.setTemplateName(template.getName());//设备的配置包含创建使用的模板的名称
        if (ObjectUtil.isNull(device.getConfig())) {
            log.info("设备config为空,使用模板config作为设备config，templateId: {}", template.getId());
            return newConfig;
        }
        Config deviceConfig = device.getConfig();
        if (ObjectUtil.isNotNull(deviceConfig.getStorageMode())) {
            newConfig.setStorageMode(deviceConfig.getStorageMode());
            log.info("设备config包含storageMode,覆盖模板config的storageMode，templateId: {}", template.getId());
        }
        return newConfig;
    }

    @Override
    public void deleteDevice(int deviceId) {
        Device device = checkPermission(deviceId);
        //删除iotdb设备
        //解除iotdb设备与存储策略对应的模板的关系
        StorageStrategy storageStrategy = storageStrategyManager.getStrategy(device.getConfig().getStorageMode());
        iotDBDao.deActiveAndUnsetSchema(
                Constants.TEMPLATE_PREFIX + device.getTemplateId() + storageStrategy.getTemplateSuffix(),
                util.getDevicePath(device.getUserId(), device.getId())
        );
        log.info("删除iotdb设备成功, deviceId: {}", deviceId);
        //删除mysql设备
        deviceMapper.delete(deviceId);
        log.info("删除设备从mysql成功, deviceId: {}", deviceId);
        log.info("删除设备成功, deviceId: {}", deviceId);
    }

    @Override
    public void clearDevice(int deviceId) {
        Device device = checkPermission(deviceId);
        //清空iotdb设备数据
        iotDBDao.clearDevice(util.getDevicePath(device.getUserId(), device.getId()));
        log.info("清空iotdb设备数据成功, deviceId: {}", deviceId);
        removeAllDataTags(deviceId);
        log.info("清空设备数据成功, deviceId: {}", deviceId);
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
    public Device addDataTags(int deviceId, Set<String> tags) {
        Device device = checkPermission(deviceId);
        Set<String> oldTags = device.getAllTags();
        if (ObjectUtil.isNull(oldTags)) {
            oldTags = new HashSet<>();
        }
        //取并集
        oldTags.addAll(tags);
        device.setAllTags(oldTags);
        deviceMapper.update(device);
        log.info("添加设备数据标签成功, deviceId: {}, tags: {}", deviceId, tags);
        return device;
    }

    @Transactional(timeout = 5)
    @Override
    public Device addDataTagsWithoutCheck(int deviceId, Set<String> tags) {
        Device device = getDeviceByIdWithoutCheck(deviceId);
        Set<String> oldTags = device.getAllTags();
        if (ObjectUtil.isNull(oldTags)) {
            oldTags = new HashSet<>();
        }
        //取并集
        oldTags.addAll(tags);
        device.setAllTags(oldTags);
        deviceMapper.update(device);
        log.info("添加设备数据标签成功, deviceId: {}, tags: {}", device.getId(), tags);
        return device;
    }

    @Override
    public Device removeDataTags(int deviceId, Set<String> tags) {
        Device device = checkPermission(deviceId);
        Set<String> oldTags = device.getAllTags();
        if (ObjectUtil.isNull(oldTags)) {
            oldTags = new HashSet<>();
        }
        //取集合差
        oldTags.removeAll(tags);
        device.setAllTags(oldTags);
        deviceMapper.update(device);
        log.info("删除设备数据标签成功, deviceId: {}, tags: {}", deviceId, tags);
        return device;
    }

    @Override
    public Device removeAllDataTags(int deviceId) {
        Device device = checkPermission(deviceId);
        device.setAllTags(new HashSet<>());
        deviceMapper.update(device);
        log.info("删除设备所有数据标签成功, deviceId: {}", deviceId);
        return device;
    }

    @Override
    public Device getDeviceById(int deviceId) {
        return checkPermission(deviceId);
    }

    private Device getDeviceByIdWithoutCheck(int deviceId) {
        return deviceMapper.selectByIdForUpdate(deviceId);
    }


    @Override
    public List<Device> getDevicesByIds(List<Integer> deviceIds) {
        List<Device> devices = new ArrayList<>();
        for (int deviceId : deviceIds) {
            devices.add(checkPermission(deviceId));
        }
        return devices;
    }


    @Override
    public List<Group> getGroupsByDeviceId(int deviceId) {
        Device device = checkPermission(deviceId);
        return groupService.getGroupByIds(device.getGroupIds());
    }

    @Override
    public List<Device> getAllDevice() {
        return deviceMapper.selectByUserId(currentUserCache.getCurrentUser().getId());
    }

    @Override
    public List<String> getAllMeasurementsById(int deviceId) {
        Device device = getDeviceById(deviceId);
        Config config = device.getConfig();
        if (ObjectUtil.isNull(config)) {
            log.warn("设备 {} 没有配置", deviceId);
            throw new SystemException(CodeMessage.DB_DATA_ROW_ERROR, "设备 " + deviceId + " 没有配置");
        }
        return config.getMeasurements();

    }
}
