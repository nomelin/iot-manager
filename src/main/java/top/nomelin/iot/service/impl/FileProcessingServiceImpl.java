package top.nomelin.iot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.common.exception.SystemException;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.dto.FileTask;
import top.nomelin.iot.model.enums.FileTaskStatus;
import top.nomelin.iot.service.FileProcessingService;
import top.nomelin.iot.service.TaskService;
import top.nomelin.iot.service.processor.FileProcessor;
import top.nomelin.iot.service.processor.FileProcessorFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;


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
    public void processAsync(String taskId, String filePath, Device device,
                             int skipRows, int mergeTimestampNum, int batchSize, String tag) {
        File file = new File(filePath);
        if (!file.exists() || file.length() == 0) {
            throw new BusinessException(CodeMessage.FILE_EMPTY_ERROR, "文件为空:" + taskId);
        }
        FileTask task = taskService.getTask(taskId);
        task.start();
        log.info("异步处理文件:，任务ID:{}, 文件名:{}, 设备:{}，文件类型:{}, skipRows:{}, mergeTimestampNum:{}, batchSize:{}",
                taskId, task.getFileName(), device, task.getFileType(), skipRows, mergeTimestampNum, batchSize);
        try (InputStream inputStream = new FileInputStream(file)) {
            FileProcessor processor = processorFactory.getProcessor(task.getFileType());
            log.info("使用{}处理器处理文件", processor.getClass().getSimpleName());
            processor.process(inputStream, device, task, skipRows, mergeTimestampNum, batchSize, tag);
            task.complete();
            log.info("文件处理完成，任务ID:{}", taskId);
        } catch (Exception e) {
            if (task.getStatus() == FileTaskStatus.CANCELLED) {
                //此异常是由用户取消任务导致的，符合预期，无需继续抛出
                log.info("任务已取消, taskId:{}", taskId);
                return;
            }
            log.error("文件处理失败，任务ID:{}, 错误信息:{}", taskId, e);
            task.fail("文件处理失败，error: " + e);
            throw new SystemException(CodeMessage.FILE_HANDLER_ERROR, "任务ID:" + taskId, e);
        } finally {
            // 处理完成后删除临时文件
            if (file.exists()) {
                if (!file.delete()) {
                    log.warn("删除临时文件失败，文件名:{}", file.getName());
                }
            }
        }
    }
}
