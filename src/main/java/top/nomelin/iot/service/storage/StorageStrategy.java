package top.nomelin.iot.service.storage;

import org.apache.iotdb.session.template.MeasurementNode;
import org.apache.tsfile.enums.TSDataType;
import top.nomelin.iot.model.dto.DeviceTable;

import java.util.List;

public interface StorageStrategy {
    /**
     * 按一定策略存储数据
     *
     * @param devicePath        设备路径
     * @param timestamps        时间戳列表
     * @param measurementsList  物理量列表
     * @param typesList         数据类型列表
     * @param valuesList        数据值列表
     * @param aggregationTime   聚合时间粒度，必须是10的整数次幂，比如1，10，100，单位为ms。
     * @param mergeTimestampNum 合并旧数据的时间戳数量(头尾)。如果等于0，则不进行合并。
     *                          如果小于0，则全部合并旧数据；如果大于0，则合并头尾各mergeTimestampNum个时间戳。
     *                          合并需要查数据库，获取旧数据。对于在每个批次都连续的数据，建议使用1。
     *                          [此参数只对并存策略有效]
     */
    void storeData(String devicePath, List<Long> timestamps, List<List<String>> measurementsList,
                   List<List<TSDataType>> typesList, List<List<Object>> valuesList, int aggregationTime,
                   int mergeTimestampNum);

    /**
     * 按和存储时相同的策略查询数据
     *
     * @param devicePath           设备路径
     * @param startTime            起始时间戳
     * @param endTime              结束时间戳
     * @param selectedMeasurements 要查询的物理量
     * @param aggregationTime      聚合时间粒度，必须是10的整数次幂，比如1，10，100，单位为ms。
     * @return 查询到的数据
     */
    DeviceTable retrieveData(String devicePath, long startTime, long endTime, List<String> selectedMeasurements,
                             int aggregationTime);

    /**
     * 对模板定义的物理量类型进行转换，以支持不同存储策略的需求。
     * 创建模板时会创建不同策略的专属模板，使用的名字后缀为 {@link #getTemplateSuffix()}。
     *
     * @param originalNodes 原始的物理量定义
     * @return 转换后的物理量定义
     */
    default List<MeasurementNode> preprocessTemplateNodes(List<MeasurementNode> originalNodes) {
        return originalNodes; // 默认不修改
    }

    /**
     * @return 模板唯一标识的后缀名，用以区分不同存储策略的模板
     */
    String getTemplateSuffix();
}

