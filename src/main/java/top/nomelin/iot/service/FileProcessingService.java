package top.nomelin.iot.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * FileProcessingService
 *
 * @author nomelin
 * @since 2025/02/04 15:32
 **/
public interface FileProcessingService {
    /**
     * 异步处理文件写入
     *
     * @param taskId   任务ID
     * @param file     文件
     * @param deviceId 设备ID
     * @param skipRows 跳过的行数(不包括表头)
     */

    void processAsync(String taskId, MultipartFile file, int deviceId, int skipRows);
}
