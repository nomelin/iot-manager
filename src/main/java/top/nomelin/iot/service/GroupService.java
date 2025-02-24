package top.nomelin.iot.service;

import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.Group;

import java.util.List;

/**
 * GroupService
 *
 * @author nomelin
 * @since 2024/12/21 21:30
 **/
public interface GroupService {
    Group checkPermission(int groupId);

    Group createGroup(Group group);

    void deleteGroup(int groupId);

    void updateGroupName(int groupId, String name);

    void updateGroupDesc(int groupId, String desc);

    void addDeviceToGroup(int groupId, List<Integer> deviceIds);

    void removeDeviceFromGroup(int groupId, List<Integer> deviceIds);

    List<Group> getAllGroup();

    Group getGroupById(int groupId);

    List<Group> getGroupByIds(List<Integer> groupIds);

    List<Device> getDevicesByGroupId(int groupId);

    /**
     * 获取某个群组的所有设备的物理量名称，取并集。
     */
    List<String> getAllMeasurement(int groupId);
}
