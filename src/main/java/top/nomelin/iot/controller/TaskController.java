package top.nomelin.iot.controller;

import cn.hutool.core.util.ObjectUtil;
import org.springframework.web.bind.annotation.*;
import top.nomelin.iot.common.Result;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.model.dto.FileTask;
import top.nomelin.iot.service.TaskService;

/**
 * TaskController
 *
 * @author nomelin
 * @since 2025/02/04 16:14
 **/
@RestController
@RequestMapping("/task")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }


    @GetMapping("/get/{taskId}")
    public Result getTask(@PathVariable String taskId) {
        if (ObjectUtil.isNull(taskId)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        FileTask task = taskService.getTask(taskId);
        return Result.success(task);
    }

    @PostMapping("/pause/{taskId}")
    public Result pauseTask(@PathVariable String taskId) {
        if (ObjectUtil.isNull(taskId)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        taskService.pauseTask(taskId);
        return Result.success();
    }

    @PostMapping("/resume/{taskId}")
    public Result resumeTask(@PathVariable String taskId) {
        if (ObjectUtil.isNull(taskId)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        taskService.resumeTask(taskId);
        return Result.success();
    }

    @PostMapping("/cancel/{taskId}")
    public Result cancelTask(@PathVariable String taskId) {
        if (ObjectUtil.isNull(taskId)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        taskService.cancelTask(taskId);
        return Result.success();
    }
}
