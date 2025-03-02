package top.nomelin.iot.dao;

import org.apache.iotdb.session.template.MeasurementNode;
import org.apache.tsfile.enums.TSDataType;
import top.nomelin.iot.model.dto.DeviceTable;

import java.util.List;
import java.util.Map;

/**
 * IoTDBDao接口，用于定义与IoTDB交互的各种数据操作方法。
 */
public interface IoTDBDao {

    /**
     * 创建一个数据库。
     *
     * @param databasePath 数据库路径。root.xxx.xxx.xxx
     */
    void createDatabase(String databasePath);

    /**
     * 创建元数据模板
     *
     * @param schemaName       元数据模板名称
     * @param measurementNodes 元数据模板的物理量定义列表
     */

    void createSchema(String schemaName, List<MeasurementNode> measurementNodes);

    /**
     * 在指定设备上挂载并激活元数据模板。激活同时会实例化设备和设备下的时间序列。
     * 注意，创建模板只需要一次，可以被多个设备使用。每次创建设备时，都需要挂载激活模板。
     *
     * @param schemaName 元数据模板名称
     * @param devicePath 设备路径
     */

    void setAndActivateSchema(String schemaName, String devicePath);

    /**
     * 从指定设备解除并卸载元数据模板。解除的同时会删除设备，设备下的时间序列，时间序列下的数据。
     * 注意，不是删除模板。一个模板可以被多个设备使用。
     *
     * @param schemaName 元数据模板名称
     * @param devicePath 设备路径
     */
    void deActiveAndUnsetSchema(String schemaName, String devicePath);

    /**
     * 删除模板。全部解除->全部卸载->删除模板。同时，删除所有使用该模板的设备，并清空所有数据。
     * 注意，删除模板会先多次解除，卸载，然后会删除模板。
     *
     * @param schemaName 模板名称
     */
    void deleteSchema(String schemaName);


    /**
     * @return 所有模板名称的列表
     */
    List<String> queryAllSchemas();

    /**
     * @param schemaName 元数据模板名称
     * @return 某模板被设置到 MTree 的所有路径的列表
     */
    List<String> queryPathsSchemaSetOn(String schemaName);

    /**
     * @param schemaName 元数据模板名称
     * @return 所有正在使用某模板的所有路径的列表
     */
    List<String> queryPathsSchemaUsingOn(String schemaName);

    /**
     * @return 指定模板内所有物理量的路径
     * <pre>
     *     例如：List<String> measurementsInSchema = dao.showMeasurementsInSchema(schemaName);
     *       >>>Measurements in schema 'testSchema': [temperature, humidity]
     * </pre>
     */
    List<String> showMeasurementsInSchema(String schemaName);

    /**
     * 插入一行对齐数据。
     *
     * @param devicePath   设备路径
     * @param time         时间戳
     * @param measurements 物理量名称列表
     * @param types        物理量数据类型列表
     * @param values       物理量数据值列表
     */

    void insertAlignedRecord(String devicePath, long time, List<String> measurements,
                             List<TSDataType> types, List<Object> values);

    /**
     * 插入多行对齐数据。
     *
     * @param devicePath       设备路径
     * @param times            时间戳列表
     * @param measurementsList 物理量名称列表列表
     * @param typesList        物理量数据类型列表列表
     * @param valuesList       物理量数据值列表列表
     */
    void insertBatchAlignedRecordsOfOneDevice(String devicePath, List<Long> times, List<List<String>> measurementsList,
                                              List<List<TSDataType>> typesList, List<List<Object>> valuesList);

    /**
     * 指定的查询时间范围为左闭右开区间，支持 start 或 end 为 null
     *
     * @param devicePath 设备路径
     * @param startTime  起始时间戳（可为 null）
     * @param endTime    结束时间戳（可为 null）
     * @return 查询结果
     */
    DeviceTable queryRecords(String devicePath, Long startTime, Long endTime);

    /**
     * 指定的查询时间范围为左闭右开区间，支持 start 或 end 为 null
     *
     * @param devicePath   设备路径
     * @param startTime    起始时间戳（可为 null）
     * @param endTime      结束时间戳（可为 null）
     * @param selectFields 需要查询的字段（物理量名称）
     * @return 查询结果
     */
    DeviceTable queryRecords(String devicePath, Long startTime, Long endTime, List<String> selectFields);


    /**
     * 删除数据库
     *
     * @param databasePath 数据库路径。
     */
    void deleteDatabase(String databasePath);

    /**
     * 清除设备的所有数据。不影响其他内容
     *
     * @param devicePath 设备路径
     */
    void clearDevice(String devicePath);

    void deleteData(String devicePath, long startTime, long endTime);

    void deleteData(String devicePath, List<Long> timestamps);


    /**
     * 查询一个时间戳下的属性及其json value值。
     */
    Map<String, String> getExistingMeasurements(String devicePath, long windowTs);

    /**
     * 批量查询时间戳下的属性及其json value值
     */
    Map<Long, Map<String, String>> getExistingMeasurementsBatch(String devicePath, List<Long> windowTimestamps);
}
