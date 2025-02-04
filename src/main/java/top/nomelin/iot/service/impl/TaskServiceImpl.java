package top.nomelin.iot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.nomelin.iot.model.dto.FileTask;
import top.nomelin.iot.service.TaskService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * TaskManagerImpl
 *
 * @author nomelin
 * @since 2025/02/04 15:48
 **/
@Service
public class TaskServiceImpl implements TaskService {
    public static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);
    private final Map<String, FileTask> tasks = new ConcurrentHashMap<>();

    @Override
    public String createTask(MultipartFile file) {
        String taskId = UUID.randomUUID().toString();
        FileTask task = new FileTask();
        task.setId(taskId);
        task.setFileName(file.getOriginalFilename());
        tasks.put(taskId, task);
        task.queue();
        log.info("Task created: " + taskId);
        return taskId;
    }

    @Override
    public FileTask getTask(String taskId) {
        return tasks.get(taskId);
    }
}
