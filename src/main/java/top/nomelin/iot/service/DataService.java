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
     * @param deviceId          设备ID
     * @param timestamps        时间戳列表
     * @param measurements      字段名列表。每个时间戳的字段都相同。
     * @param tag               标签，每批次写入的数据都是相同的标签。如果为null，代表没有标签。
     *                          限制：不能为"NO_TAG"，不能包含"||"
     * @param values            数据值列表，每行代表一个时间戳的记录。
     * @param mergeTimestampNum 合并旧数据的时间戳数量(头尾)。如果等于0，则不进行合并。
     *                          如果小于0，则全部合并旧数据；如果大于0，则合并头尾各mergeTimestampNum个时间戳。
     *                          合并需要查数据库，获取旧数据。对于在每个批次都连续的数据，建议使用1。
     *                          [此参数只对并存策略有效]
     */
    void insertBatchRecord(int deviceId, List<Long> timestamps, String tag,
                           List<String> measurements, List<List<Object>> values, int mergeTimestampNum);

    /**
     * 批量插入记录。插入时的配置信息从device对象中获取, 不查询mysql。
     * 在没有session用户缓存的情况下（比如异步任务），必须使用该方法，否则无法查询设备。
     *
     * @param device            设备对象
     * @param timestamps        时间戳列表
     * @param tag               标签，每批次写入的数据都是相同的标签。如果为null，代表没有标签。
     *                          限制：不能为"NO_TAG"，不能包含"||"
     * @param measurements      字段名列表。每个时间戳的字段都相同。
     * @param values            数据值列表，每行代表一个时间戳的记录。
     * @param mergeTimestampNum 合并旧数据的时间戳数量(头尾)。如果等于0，则不进行合并。
     *                          如果小于0，则全部合并旧数据；如果大于0，则合并头尾各mergeTimestampNum个时间戳。
     *                          合并需要查数据库，获取旧数据。对于在每个批次都连续的数据，建议使用1。
     *                          [此参数只对并存策略有效]
     */
    void insertBatchRecord(Device device, List<Long> timestamps, String tag,
                           List<String> measurements, List<List<Object>> values, int mergeTimestampNum);

    /**
     * 查询记录。查询时的配置信息需要传入。（不从view表中获取配置信息，因为要支持外部非视图查询）
     * 查询时会先应用阈值过滤，然后再聚合数据。
     *
     * @param deviceId           设备ID
     * @param startTime          开始时间戳，可以为null，表示不限制
     * @param endTime            结束时间戳，可以为null，表示不限制
     * @param selectMeasurements 选择的属性名列表。如果为null或空列表，则查询所有属性。
     * @param tagQuery           用此标签过滤。如果为null或空字符串，则不过滤。如果为"NO_TAG"，则筛选没有标签的数据。
     *                           可以同时筛选多个标签，使用||分割。
     *                           例如：
     *                           null,表示不过滤。
     *                           "",表示不过滤。
     *                           "tag1",表示筛选标签为tag1的数据。
     *                           "tag1||tag2",表示筛选标签为tag1或tag2的数据。
     *                           "NO_TAG",表示筛选没有标签的数据。
     *                           "tag1||"NO_TAG",表示筛选没有标签或标签为tag1的数据。
     * @param aggregationTime    聚合时间粒度,单位：ms。读取粒度不能小于存储粒度。为null时，不聚合; 小于1时，不聚合。
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

    DeviceTable queryRecord(int deviceId, Long startTime, Long endTime, List<String> selectMeasurements, String tagQuery,
                            Integer aggregationTime, QueryAggregateFunc queryAggregateFunc, List<List<Double>> thresholds);

    /**
     * 查询记录。查询时的配置信息需要传入。（不从view表中获取配置信息，因为要支持外部非视图查询）
     * 插入时的配置信息从device对象中获取, 不查询mysql。
     * 查询时会先应用阈值过滤，然后再聚合数据。
     *
     * @param device             设备对象
     * @param startTime          开始时间戳，可以为null，表示不限制
     * @param endTime            结束时间戳，可以为null，表示不限制
     * @param selectMeasurements 选择的属性名列表。如果为null或空列表，则查询所有属性。
     * @param tagQuery           用此标签过滤。如果为null或空字符串，则不过滤。如果为"NO_TAG"，则筛选没有标签的数据。
     *                           可以同时筛选多个标签，使用||分割。
     *                           例如：
     *                           null,表示不过滤。
     *                           "",表示不过滤。
     *                           "tag1",表示筛选标签为tag1的数据。
     *                           "tag1||tag2",表示筛选标签为tag1或tag2的数据。
     *                           "NO_TAG",表示筛选没有标签的数据。
     *                           "tag1||"NO_TAG",表示筛选没有标签或标签为tag1的数据。
     * @param aggregationTime    聚合时间粒度,单位：ms。读取粒度不能小于存储粒度。为null时，不聚合; 小于1时，不聚合。
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
    DeviceTable queryRecord(Device device, Long startTime, Long endTime, List<String> selectMeasurements, String tagQuery,
                            Integer aggregationTime, QueryAggregateFunc queryAggregateFunc, List<List<Double>> thresholds);

}
