package top.nomelin.iot.controller;

import cn.hutool.core.util.ObjectUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.nomelin.iot.common.Result;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.service.FileProcessingService;
import top.nomelin.iot.service.TaskService;

/**
 * FileController
 *
 * @author nomelin
 * @since 2025/02/04 15:33
 **/
@RestController
@RequestMapping("/files")
public class FileController {
    private final FileProcessingService processingService;
    private final TaskService taskService;

    public FileController(FileProcessingService processingService, TaskService taskService) {
        this.processingService = processingService;
        this.taskService = taskService;
    }

    @PostMapping("/upload")
    public Result uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("skipRows") int skipRows,
            @RequestParam("deviceId") int deviceId) {
        if (file.isEmpty()) {
            throw new BusinessException(CodeMessage.PARAM_ERROR, "文件为空");
        }
        if (ObjectUtil.isNull(skipRows) || ObjectUtil.isNull(deviceId)) {
            throw new BusinessException(CodeMessage.PARAM_LOST_ERROR);
        }
        if (skipRows < 0 || deviceId < 0) {
            throw new BusinessException(CodeMessage.PARAM_ERROR,
                    String.format("参数错误: skipRows=%d, deviceId=%d", skipRows, deviceId));
        }
        String taskId = taskService.createTask(file);
        processingService.processAsync(taskId, file, deviceId, skipRows);
        return Result.success(taskId);
    }
}
