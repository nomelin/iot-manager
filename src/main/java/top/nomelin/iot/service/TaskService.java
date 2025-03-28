package top.nomelin.iot.service;

import org.springframework.web.multipart.MultipartFile;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.dto.FileTask;

import java.util.List;

/**
 * TaskManager
 *
 * @author nomelin
 * @since 2025/02/04 15:33
 **/
public interface TaskService {
//    void updateTaskProgress(String taskId, int processed);

    String createTask(MultipartFile file, Device device, String tag);

    void pauseTask(String taskId);

    void resumeTask(String taskId);

    void cancelTask(String taskId);

    FileTask getTask(String taskId);

    List<String> getAllTaskIds();
}
