package top.nomelin.iot.service.impl;

import cn.hutool.core.util.ObjectUtil;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import top.nomelin.iot.cache.CurrentUserCache;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.dao.GroupMapper;
import top.nomelin.iot.model.Group;
import top.nomelin.iot.service.DeviceService;
import top.nomelin.iot.service.GroupService;

import java.util.List;
import java.util.Objects;

/**
 * GroupServiceImpl
 *
 * @author nomelin
 * @since 2024/12/21 21:43
 **/
@Service
public class GroupServiceImpl implements GroupService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(GroupServiceImpl.class);
    private final GroupMapper groupMapper;

    private final DeviceService deviceService;

    private final CurrentUserCache currentUserCache;

    public GroupServiceImpl(GroupMapper groupMapper, DeviceService deviceService, CurrentUserCache currentUserCache) {
        this.groupMapper = groupMapper;
        this.deviceService = deviceService;
        this.currentUserCache = currentUserCache;
    }

    /**
     * 检查当前用户是否有权限访问该group，并返回group对象
     */
    @Override
    public Group checkPermission(int groupId) {
        Group group = groupMapper.selectById(groupId);
        if (ObjectUtil.isNull(group)) {
            log.warn("group不存在，groupId={}", groupId);
            throw new BusinessException(CodeMessage.NOT_FOUND_ERROR);
        }
        if (!Objects.equals(group.getUserId(), currentUserCache.getCurrentUser().getId())) {
            log.warn("当前用户没有权限访问该group，groupId={}, userId={}", groupId, currentUserCache.getCurrentUser().getId());
            throw new BusinessException(CodeMessage.NO_PERMISSION_ERROR);
        }
        return group;
    }

    /**
     * 检查当前用户是否有权限访问该group，同时检查是否有权限访问所有设备,并返回group对象
     */
    private Group checkPermission(int groupId, List<Integer> deviceIds) {
        for (int deviceId : deviceIds) {
            deviceService.checkPermission(deviceId);
        }
        return checkPermission(groupId);
    }

    @Override
    public Group createGroup(Group group) {
        group.setUserId(currentUserCache.getCurrentUser().getId());
        groupMapper.insert(group);
        log.info("创建group成功，group={}", group);
        return group;
    }

    @Override
    public void deleteGroup(int groupId) {
        checkPermission(groupId);
        groupMapper.delete(groupId);
        log.info("删除group成功，groupId={}", groupId);
    }

    @Override
    public void updateGroupName(int groupId, String name) {
        checkPermission(groupId);
        Group group = new Group();
        group.setId(groupId);
        group.setName(name);
        groupMapper.update(group);
        log.info("更新group名称成功，groupId={}, name={}", groupId, name);
    }

    @Override
    public void updateGroupDesc(int groupId, String desc) {
        checkPermission(groupId);
        Group group = new Group();
        group.setId(groupId);
        group.setDescription(desc);
        groupMapper.update(group);
        log.info("更新group描述成功，groupId={}, desc={}", groupId, desc);
    }

    @Override
    public void addDeviceToGroup(int groupId, List<Integer> deviceIds) {
        Group group = checkPermission(groupId, deviceIds);
        //去重
        List<Integer> oldDeviceIds = group.getDeviceIds();
        deviceIds.removeAll(oldDeviceIds);
        if (deviceIds.isEmpty()) {
            log.info("设备已全部在group中，不再重复添加，groupId={}, deviceIds={}", groupId, deviceIds);
            return;
        }
        //检查设备是否存在
        groupMapper.insertGroupDeviceRelations(groupId, deviceIds);
        log.info("添加设备到group成功，groupId={}, deviceIds={}", groupId, deviceIds);
    }

    @Override
    public void removeDeviceFromGroup(int groupId, List<Integer> deviceIds) {
        Group group = checkPermission(groupId, deviceIds);
        //去除组内不存在的设备
        List<Integer> oldDeviceIds = group.getDeviceIds();
        deviceIds.retainAll(oldDeviceIds);//保留组内存在的设备
        if (deviceIds.isEmpty()) {
            log.info("设备全部不在group中，不能移除，groupId={}, deviceIds={}", groupId, deviceIds);
            return;
        }
        groupMapper.deleteGroupDeviceRelations(groupId, deviceIds);
        log.info("从group移除设备成功，groupId={}, deviceIds={}", groupId, deviceIds);
    }

    @Override
    public List<Group> getAllGroup() {
        Group group = new Group();
        group.setUserId(currentUserCache.getCurrentUser().getId());
        return groupMapper.selectAll(group);
    }

    @Override
    public Group getGroupById(int groupId) {
        return checkPermission(groupId);
    }
}
