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
     *
     * @param inputStream       文件输入流
     * @param device            设备信息
     * @param task              处理任务上下文
     * @param skipRows          要跳过的行数(不包括表头行)
     * @param mergeTimestampNum 合并旧数据的时间戳数量(头尾)。如果等于0，则不进行合并。
     *                          如果小于0，则全部合并旧数据；如果大于0，则合并头尾各mergeTimestampNum个时间戳。
     *                          合并需要查数据库，获取旧数据。对于在每个批次都连续的数据，建议使用1。
     *                          [此参数只对并存策略有效]
     * @param batchSize         每批次处理的文件行数。调下游服务写入数据时，一次处理的行数。
     */
    void process(InputStream inputStream, Device device, FileTask task,
                 int skipRows, int mergeTimestampNum, int batchSize) throws IOException;

    String getSupportedType();
}
