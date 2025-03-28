package top.nomelin.iot.service;

import top.nomelin.iot.model.Device;

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
     * @param taskId                   任务ID
     * @param filePath                 文件路径
     * @param device                   设备信息
     * @param skipRows                 跳过的行数(不包括表头)
     * @param mergeTimestampNum        合并旧数据的时间戳数量(头尾)。如果等于0，则不进行合并。
     *                                 如果小于0，则全部合并旧数据；如果大于0，则合并头尾各mergeTimestampNum个时间戳。
     *                                 合并需要查数据库，获取旧数据。对于在每个批次都连续的数据，建议使用1。
     *                                 [此参数只对并存策略有效]
     * @param batchSize                每批次处理的文件行数。调下游服务写入数据时，一次处理的行数。
     * @param tag                      标签，每次写入的数据都是相同的标签。如果为null，代表没有标签。
     * @param autoHandleErrorTimeStamp 自动处理无法解析的时间戳对应的数据行：
     *                                 如果为0，直接抛出异常。
     *                                 如果为1，尝试将时间戳修改为上一个数据行的时间戳，如果无法修改，则抛出异常。
     *                                 如果为2，尝试将时间戳修改为上一个数据行的时间戳，如果无法修改，则删除此行。
     *                                 如果为3，直接删除此行。
     */

    void processAsync(String taskId, String filePath, Device device, int skipRows, int mergeTimestampNum,
                      int batchSize, String tag, int autoHandleErrorTimeStamp);
}
