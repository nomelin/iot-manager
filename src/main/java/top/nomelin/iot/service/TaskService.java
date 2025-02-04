package top.nomelin.iot.service;

import org.springframework.web.multipart.MultipartFile;
import top.nomelin.iot.model.dto.FileTask;

/**
 * TaskManager
 *
 * @author nomelin
 * @since 2025/02/04 15:33
 **/
public interface TaskService {
    String createTask(MultipartFile file);

//    void updateTaskProgress(String taskId, int processed);

    FileTask getTask(String taskId);
}
