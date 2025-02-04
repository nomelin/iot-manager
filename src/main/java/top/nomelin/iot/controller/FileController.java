package top.nomelin.iot.controller;

import cn.hutool.core.util.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import top.nomelin.iot.common.Result;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.service.DeviceService;
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
    public static final Logger log = LoggerFactory.getLogger(FileController.class);
    private final FileProcessingService processingService;
    private final TaskService taskService;
    private final DeviceService deviceService;

    public FileController(FileProcessingService processingService, TaskService taskService, DeviceService deviceService) {
        this.processingService = processingService;
        this.taskService = taskService;
        this.deviceService = deviceService;
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
        Device device = deviceService.getDeviceById(deviceId);
        if (ObjectUtil.isNull(device)) {
            throw new BusinessException(CodeMessage.DEVICE_NOT_EXIST_ERROR, "设备不存在");
        }
        String taskId = taskService.createTask(file);
        //异步处理文件，必须先查询Device对象并传入异步方法，否则由于session用户缓存不存在，异步方法内无法获取到设备信息
        processingService.processAsync(taskId, file, device, skipRows);
        log.info("上传文件成功, 异步处理任务，直接返回。taskId: {}", taskId);
        return Result.success(taskId);
    }
}
