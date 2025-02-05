package top.nomelin.iot.service.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.SystemException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FileProcessorFactory
 *
 * @author nomelin
 * @since 2025/02/04 15:34
 **/
@Component
public class FileProcessorFactory {
    private final Map<String, FileProcessor> processors = new ConcurrentHashMap<>();

    @Autowired
    public FileProcessorFactory(List<FileProcessor> processorList) {
        processorList.forEach(p ->
                processors.put(p.getSupportedType(), p));
    }

    public FileProcessor getProcessor(String fileType) {
        if (!processors.containsKey(fileType.toLowerCase())) {
            throw new SystemException(CodeMessage.UNSUPPORTED_FILE_TYPE_ERROR, "不支持的文件类型: " + fileType);
        }
        return processors.get(fileType.toLowerCase());
    }
}
