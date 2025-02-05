package top.nomelin.iot.service.processor;

import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.dto.FileTask;

import java.io.IOException;
import java.io.InputStream;

/**
 * FileProcessor
 *
 * @author nomelin
 * @since 2025/02/04 12:44
 **/
public interface FileProcessor {
    /**
     * 处理文件流
     * @param inputStream 文件输入流
     * @param device 设备信息
     * @param task 处理任务上下文
     * @param skipRows 要跳过的行数(不包括表头行)
     */
    void process(InputStream inputStream, Device device, FileTask task, int skipRows) throws IOException;

    String getSupportedType();
}
