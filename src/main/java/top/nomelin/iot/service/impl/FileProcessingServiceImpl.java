package top.nomelin.iot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.common.exception.SystemException;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.dto.FileTask;
import top.nomelin.iot.service.FileProcessingService;
import top.nomelin.iot.service.TaskService;
import top.nomelin.iot.service.processor.FileProcessor;
import top.nomelin.iot.service.processor.FileProcessorFactory;


/**
 * FileProcessingServiceImpl
 *
 * @author nomelin
 * @since 2025/02/04 12:45
 **/
@Service
public class FileProcessingServiceImpl implements FileProcessingService {
    private static final Logger log = LoggerFactory.getLogger(FileProcessingServiceImpl.class);
    private final TaskService taskService;
    private final FileProcessorFactory processorFactory;

    public FileProcessingServiceImpl(TaskService taskService, FileProcessorFactory processorFactory) {
        this.taskService = taskService;
        this.processorFactory = processorFactory;
    }

    @Async
    @Override
    public void processAsync(String taskId, MultipartFile file, Device device, int skipRows) {
        FileTask task = taskService.getTask(taskId);
        task.start();
        log.info("异步处理文件:，任务ID:{}, 文件名:{}, 设备:{}，文件类型:{}",
                taskId, file.getOriginalFilename(), device, file.getContentType());
        try {
            FileProcessor processor = processorFactory.getProcessor(file.getContentType());
            log.info("使用{}处理器处理文件", processor.getClass().getSimpleName());
            processor.process(file.getInputStream(), device, task, skipRows);
            task.complete();
            log.info("文件处理完成，任务ID:{}", taskId);
        } catch (Exception e) {
            log.error("文件处理失败，任务ID:{}, 错误信息:{}", taskId, e);
            task.fail("文件处理失败，error: " + e);
            throw new SystemException(CodeMessage.FILE_HANDLER_ERROR, "任务ID:" + taskId, e);
        }
    }
}
