package top.nomelin.iot.service;

import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.Group;

import java.util.List;
import java.util.Set;

/**
 * DeviceService
 *
 * @author nomelin
 * @since 2024/12/21 22:13
 **/
public interface DeviceService {
    Device checkPermission(int deviceId);

    Device addDevice(Device device, int templateId);

    /**
     * 删除设备
     */
    void deleteDevice(int deviceId);

    /**
     * 清空设备数据
     */
    void clearDevice(int deviceId);

    void updateDeviceName(int deviceId, String name);

    Device addTags(int deviceId, List<String> tags);

    Device removeTags(int deviceId, List<String> tags);

    /**
     * 添加数据行标签记录
     */
    Device addDataTags(int deviceId, Set<String> tags);

    Device addDataTagsWithoutCheck(int deviceId, Set<String> tags);

    /**
     * 删除数据行标签记录
     */
    Device removeDataTags(int deviceId, Set<String> tags);

    Device removeAllDataTags(int deviceId);

    Device getDeviceById(int deviceId);

    Device getDeviceByIdWithoutCheck(int deviceId);

    List<Device> getDevicesByIds(List<Integer> deviceIds);

    List<Group> getGroupsByDeviceId(int deviceId);

    List<Device> getDevicesByTemplateId(int templateId);

    /**
     * @return 当前登录用户的所有设备
     */
    List<Device> getAllDevice();

    List<String> getAllMeasurementsById(int deviceId);
}
