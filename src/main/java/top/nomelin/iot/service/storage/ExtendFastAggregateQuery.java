package top.nomelin.iot.service.storage;

import top.nomelin.iot.model.dto.DeviceTable;
import top.nomelin.iot.model.enums.QueryAggregateFunc;

import java.util.List;

/**
 * 扩展接口-快速聚合查询，如果一个存储策略支持快速聚合查询，则实现此接口。系统会优先调用此接口的聚合查询方法，
 * 而不是 {@link StorageStrategy#retrieveData} 方法。如果失败，才会call {@link StorageStrategy#retrieveData} 方法。
 **/
public interface ExtendFastAggregateQuery {

    DeviceTable fastAggregateQuery(String devicePath, Long startTime, Long endTime, List<String> selectedMeasurements,
                                   int aggregationTime, QueryAggregateFunc queryAggregateFunc);

}
