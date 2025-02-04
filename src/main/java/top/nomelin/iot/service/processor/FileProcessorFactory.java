package top.nomelin.iot.service.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        return processors.get(fileType.toLowerCase());
    }
}
