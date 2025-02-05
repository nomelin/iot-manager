package top.nomelin.iot.service;

import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.dto.DeviceTable;
import top.nomelin.iot.model.enums.QueryAggregateFunc;

import java.util.List;

/**
 * DataService
 *
 * @author nomelin
 * @since 2025/01/27 17:49
 **/
public interface DataService {

//    void insertRecord(int deviceId, long timestamp,
//                      List<String> measurements, List<IotDataType> types, List<Object> values);

    /**
     * 批量插入记录。插入时的配置信息会从device表中获取, 每次插入都要查询一次mysql。
     *
     * @param deviceId     设备ID
     * @param timestamps   时间戳列表
     * @param measurements 字段名列表。每个时间戳的字段都相同。
     * @param values       数据值列表，每行代表一个时间戳的记录。
     */
    void insertBatchRecord(int deviceId, List<Long> timestamps,
                           List<String> measurements, List<List<Object>> values);

    /**
     * 批量插入记录。插入时的配置信息从device对象中获取, 不查询mysql。
     * 在没有session用户缓存的情况下（比如异步任务），必须使用该方法，否则无法查询设备。
     *
     * @param device       设备对象
     * @param timestamps   时间戳列表
     * @param measurements 字段名列表。每个时间戳的字段都相同。
     * @param values       数据值列表，每行代表一个时间戳的记录。
     */
    void insertBatchRecord(Device device, List<Long> timestamps,
                           List<String> measurements, List<List<Object>> values);

    /**
     * 查询记录。查询时的配置信息需要传入。（不从view表中获取配置信息，因为要支持外部非视图查询）
     * 查询时会先应用阈值过滤，然后再聚合数据。
     *
     * @param deviceId           设备ID
     * @param startTime          开始时间戳
     * @param endTime            结束时间戳
     * @param selectMeasurements 选择的属性名列表。如果为null，则查询所有属性。
     * @param aggregationTime    聚合时间,单位：ms。读取粒度不能小于存储粒度。为null时，不聚合; 小于1时，不聚合。
     * @param queryAggregateFunc 查询聚合模式。为null时，不聚合。
     * @param thresholds         阈值列表。如果为null，则不进行阈值过滤。按照属性名顺序排列。如果查询模式为COUNT，则该参数无效。
     *                           如果selectMeasurements为null，则该参数列表数量应为全部属性数量。
     *                           如果selectMeasurements不为null，则该参数列表数量等于selectMeasurements的数量。
     *                           不需要过滤的属性阈值设置为null，也可以对min，max分别设置null，表示只按大或小值过滤。
     *                           例如：
     *                           null,表示不过滤。
     *                           [[100, 200], [120, 250],...],表示100<=属性1<=200，120<=属性2<=250，...
     *                           [[null, 100], [200,null],...],表示属性1<=100，属性2>=200，...
     *                           [null,[100,200],[null,100]]，表示属性1不过滤,100<=属性2<=200，属性3<=100
     */

    DeviceTable queryRecord(int deviceId, long startTime, long endTime, List<String> selectMeasurements,
                            Integer aggregationTime, QueryAggregateFunc queryAggregateFunc, List<List<Double>> thresholds);
    //TODO 每次查询都需要先查询设备配置表来获取写入时的配置，是否有更好的方式？比如缓存? 前端传入？前端传入可以重载一个方法。

}
