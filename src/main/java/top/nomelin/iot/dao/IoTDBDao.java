package top.nomelin.iot.dao;

import org.apache.iotdb.session.template.MeasurementNode;
import org.apache.tsfile.enums.TSDataType;
import top.nomelin.iot.model.DeviceTable;

import java.util.List;

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
     * 挂载并激活元数据模板。激活同时会实例化设备和设备下的时间序列。
     *
     * @param schemaName   元数据模板名称
     * @param databasePath 数据库名称
     * @param deviceName   设备名称
     */

    void setAndActivateSchema(String schemaName, String databasePath, String deviceName);

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
     * 指定的查询时间范围为左闭右开区间
     *
     * @param devicePath 设备路径
     * @param startTime  起始时间戳
     * @param endTime    结束时间戳
     * @return 查询结果
     */
    DeviceTable queryRecords(String devicePath, long startTime, long endTime);

    /**
     * 指定的查询时间范围为左闭右开区间
     *
     * @param devicePath   设备路径
     * @param startTime    起始时间戳
     * @param endTime      结束时间戳
     * @param selectFields 需要查询的字段（物理量名称）
     * @return 查询结果
     */
    DeviceTable queryRecords(String devicePath, long startTime, long endTime, List<String> selectFields);


    /**
     * 删除数据库
     *
     * @param databasePath 数据库路径。
     */
    void deleteDatabase(String databasePath);


}
