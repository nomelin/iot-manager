package top.nomelin.iot.controller;

import cn.hutool.core.util.ObjectUtil;
import org.springframework.web.bind.annotation.*;
import top.nomelin.iot.common.Result;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.model.Group;
import top.nomelin.iot.service.GroupService;

import java.util.List;

/**
 * GroupController
 *
 * @author nomelin
 * @since 2024/12/20 19:59
 **/
@RestController
@RequestMapping("/group")
public class GroupController {
    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @RequestMapping("/add")
    public Result addGroup(@RequestBody Group group) {
        if (ObjectUtil.isNull(group) || ObjectUtil.isEmpty(group.getName())) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        return Result.success(groupService.createGroup(group));
    }

    @RequestMapping("/delete/{groupId}")
    public Result deleteGroup(@PathVariable Integer groupId) {
        if (ObjectUtil.isNull(groupId)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        groupService.deleteGroup(groupId);
        return Result.success();
    }

    @RequestMapping("/updateName/{groupId}")
    public Result updateGroupName(@PathVariable Integer groupId, @RequestParam String name) {
        if (ObjectUtil.isNull(groupId) || ObjectUtil.isEmpty(name)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        groupService.updateGroupName(groupId, name);
        return Result.success();
    }

    @RequestMapping("/updateDesc/{groupId}")
    public Result updateGroupDesc(@PathVariable Integer groupId, @RequestParam String desc) {
        if (ObjectUtil.isNull(groupId) || ObjectUtil.isEmpty(desc)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        groupService.updateGroupDesc(groupId, desc);
        return Result.success();
    }

    @RequestMapping("/addDevices/{groupId}")
    public Result addDevicesToGroup(@PathVariable Integer groupId, @RequestBody List<Integer> deviceIds) {
        if (ObjectUtil.isNull(groupId) || ObjectUtil.isEmpty(deviceIds)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        groupService.addDeviceToGroup(groupId, deviceIds);
        return Result.success();
    }

    @RequestMapping("/removeDevices/{groupId}")
    public Result removeDevicesFromGroup(@PathVariable Integer groupId, @RequestBody List<Integer> deviceIds) {
        if (ObjectUtil.isNull(groupId) || ObjectUtil.isEmpty(deviceIds)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        groupService.removeDeviceFromGroup(groupId, deviceIds);
        return Result.success();
    }

    @RequestMapping("/get/{groupId}")
    public Result getGroupById(@PathVariable Integer groupId) {
        if (ObjectUtil.isNull(groupId)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        return Result.success(groupService.getGroupById(groupId));
    }

    @RequestMapping("/all")
    public Result getAllGroups() {
        return Result.success(groupService.getAllGroup());
    }

    @RequestMapping("/getDevices/{groupId}")
    public Result getDevicesByGroupId(@PathVariable Integer groupId) {
        if (ObjectUtil.isNull(groupId)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        return Result.success(groupService.getDevicesByGroupId(groupId));
    }
}
