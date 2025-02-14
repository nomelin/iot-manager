package top.nomelin.iot.service;

import top.nomelin.iot.model.Device;

import java.util.List;

/**
 * DeviceService
 *
 * @author nomelin
 * @since 2024/12/21 22:13
 **/
public interface DeviceService {
    Device checkPermission(int deviceId);

    Device addDevice(Device device, int templateId);

    void deleteDevice(int deviceId);

    void updateDeviceName(int deviceId, String name);

    Device addTags(int deviceId, List<String> tags);

    Device removeTags(int deviceId, List<String> tags);

    Device getDeviceById(int deviceId);

    List<Device> getDevicesByIds(List<Integer> deviceIds);

    /**
     * @return 当前登录用户的所有设备
     */
    List<Device> getAllDevice();

    List<String> getAllMeasurementsById(int deviceId);
}
