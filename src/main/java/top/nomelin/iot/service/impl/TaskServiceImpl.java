package top.nomelin.iot.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.nomelin.iot.common.Constants;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.dto.FileTask;
import top.nomelin.iot.service.TaskService;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
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
    private final Map<String, TaskMetadata> tasks = new ConcurrentHashMap<>();

    @Value("${task.auto-clean.fail_expired_days:1}")
    private int autoCleanFailExpiredDays;

    @Value("${task.auto-clean.ongoing_expired_days:7}")
    private int autoCleanOngoingExpiredDays;

    @Override
    public String createTask(MultipartFile file, Device device, String tag) {
        String taskId = UUID.randomUUID().toString();
        FileTask task = new FileTask();
        task.setId(taskId);
        task.setFileName(file.getOriginalFilename());
        task.setDevice(device); // 设置设备信息
        task.setTag(tag);
        task.queue();
        tasks.put(taskId, new TaskMetadata(task, LocalDateTime.now()));
        log.info("任务创建: {}", taskId);
        return taskId;
    }

    @Override
    public void pauseTask(String taskId) {
        TaskMetadata metadata = tasks.get(taskId);
        if (metadata == null) {
            log.warn("任务未找到: {}", taskId);
            throw new BusinessException(CodeMessage.TASK_NOT_EXIST_ERROR, "任务未找到: " + taskId);
        }
        FileTask task = metadata.task;
        task.pause();
        log.info("任务暂停: {}", taskId);
    }

    @Override
    public void resumeTask(String taskId) {
        TaskMetadata metadata = tasks.get(taskId);
        if (metadata == null) {
            log.warn("任务未找到: {}", taskId);
            throw new BusinessException(CodeMessage.TASK_NOT_EXIST_ERROR, "任务未找到: " + taskId);
        }
        FileTask task = metadata.task;
        task.resume();
        log.info("任务恢复: {}", taskId);
    }

    @Override
    public void cancelTask(String taskId) {
        TaskMetadata metadata = tasks.get(taskId);
        if (metadata == null) {
            log.warn("任务未找到: {}", taskId);
            throw new BusinessException(CodeMessage.TASK_NOT_EXIST_ERROR, "任务未找到: " + taskId);
        }
        FileTask task = metadata.task;
        task.cancel();
        log.info("任务取消: {}", taskId);
    }

    @Override
    public FileTask getTask(String taskId) {
        TaskMetadata metadata = tasks.get(taskId);
        if (metadata == null) {
            log.warn("任务未找到: {}", taskId);
            throw new BusinessException(CodeMessage.TASK_NOT_EXIST_ERROR, "任务未找到: " + taskId);
        }
        log.info("任务获取: {}", taskId);
        return metadata.task;
    }

    @Override
    public List<String> getAllTaskIds() {
        return tasks.keySet().stream().toList();
    }

    /**
     * 定时清理过期任务（每5min执行一次）
     */
    @Scheduled(fixedRate = Constants.TASK_AUTO_CLEAN_INTERVAL)
    public void cleanupExpiredTasks() {
        log.info("开始清理过期任务...");
        LocalDateTime now = LocalDateTime.now();
        Iterator<Map.Entry<String, TaskMetadata>> iterator = tasks.entrySet().iterator();
        int cleanedCount = 0;

        while (iterator.hasNext()) {
            Map.Entry<String, TaskMetadata> entry = iterator.next();
            String taskId = entry.getKey();
            TaskMetadata metadata = entry.getValue();
            FileTask task = metadata.task;
            LocalDateTime createdAt = metadata.createdAt;

            try {
                switch (task.getStatus()) {
                    case COMPLETED:
                        iterator.remove();
                        log.debug("清理成功任务: {}", taskId);
                        cleanedCount++;
                        break;
                    case FAILED:
                    case CANCELLED:
                        if (shouldCleanFinalState(task, now, autoCleanFailExpiredDays)) {
                            iterator.remove();
                            log.info("清理失败/取消任务(超过1天): {}", taskId);
                            cleanedCount++;
                        }
                        break;
                    case QUEUED:
                    case PROCESSING:
                    case PAUSED:
                        if (shouldCleanOngoingTask(createdAt, now, autoCleanOngoingExpiredDays)) {
                            iterator.remove();
                            log.info("清理长期未完成任务(超过7天): {}", taskId);
                            cleanedCount++;
                        }
                        break;
                    default:
                        log.warn("未知任务状态: {}", task.getStatus());
                }
            } catch (Exception e) {
                log.error("清理任务异常: {}", taskId, e);
            }
        }

        log.debug("任务清理完成，共清理{}个任务，剩余任务数: {}", cleanedCount, tasks.size());
    }

    /**
     * 判断终止状态任务是否需要清理
     */
    private boolean shouldCleanFinalState(FileTask task, LocalDateTime now, int expireDays) {
        LocalDateTime endTime = task.getEndTime();
        return endTime != null && endTime.plusDays(expireDays).isBefore(now);
    }

    /**
     * 判断进行中任务是否需要清理
     */
    private boolean shouldCleanOngoingTask(LocalDateTime createdAt, LocalDateTime now, int expireDays) {
        return createdAt.plusDays(expireDays).isBefore(now);
    }

    // 任务元数据内部类，用于记录任务创建时间
    private static class TaskMetadata {
        FileTask task;
        LocalDateTime createdAt;

        TaskMetadata(FileTask task, LocalDateTime createdAt) {
            this.task = task;
            this.createdAt = createdAt;
        }
    }
}