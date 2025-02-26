package top.nomelin.iot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.model.dto.FileTask;
import top.nomelin.iot.service.TaskService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TaskManagerImpl
 *
 * @author nomelin
 * @since 2025/02/04 15:48
 **/
@Service
public class TaskServiceImpl implements TaskService {
    public static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    //目前的任务只支持FileTask。如果要扩展，需要设置不同的工厂方法。
    private final Map<String, FileTask> tasks = new ConcurrentHashMap<>();

    @Override
    public String createTask(MultipartFile file) {
        String taskId = UUID.randomUUID().toString();
        FileTask task = new FileTask();
        task.setId(taskId);
        task.setFileName(file.getOriginalFilename());
        tasks.put(taskId, task);
        task.queue();
        log.info("任务创建: " + taskId);
        return taskId;
    }

    @Override
    public void pauseTask(String taskId) {
        FileTask task = tasks.get(taskId);
        if (task == null) {
            log.warn("任务未找到: " + taskId);
            throw new BusinessException(CodeMessage.TASK_NOT_EXIST_ERROR, "任务未找到: " + taskId);
        }
        task.pause();
        log.info("任务暂停: " + taskId);
    }

    @Override
    public void resumeTask(String taskId) {
        FileTask task = tasks.get(taskId);
        if (task == null) {
            log.warn("任务未找到: " + taskId);
            throw new BusinessException(CodeMessage.TASK_NOT_EXIST_ERROR, "任务未找到: " + taskId);
        }
        task.resume();
        log.info("任务恢复: " + taskId);
    }

    @Override
    public void cancelTask(String taskId) {
        FileTask task = tasks.get(taskId);
        if (task == null) {
            log.warn("任务未找到: " + taskId);
            throw new BusinessException(CodeMessage.TASK_NOT_EXIST_ERROR, "任务未找到: " + taskId);
        }
        task.cancel();
        log.info("任务取消: " + taskId);
    }

    @Override
    public FileTask getTask(String taskId) {
        FileTask task = tasks.get(taskId);
        if (task == null) {
            log.warn("任务未找到: " + taskId);
            throw new BusinessException(CodeMessage.TASK_NOT_EXIST_ERROR, "任务未找到: " + taskId);
        }
        log.info("任务获取: " + taskId);
        return task;
    }
}
