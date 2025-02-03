package top.nomelin.iot.service.storage;

import org.springframework.stereotype.Component;
import top.nomelin.iot.common.enums.StorageMode;
import top.nomelin.iot.service.storage.impl.CompatibleStorageStrategy;
import top.nomelin.iot.service.storage.impl.CoverStorageStrategy;
import top.nomelin.iot.service.storage.impl.PerformanceStorageStrategy;

import java.util.List;
import java.util.Map;

/**
 * 存储策略管理器
 *
 * @author nomelin
 * @since 2025/02/03 12:56
 **/
@Component
public class StorageStrategyManager {
    private final Map<StorageMode, StorageStrategy> storageStrategies;// 存储策略，策略模式

    public StorageStrategyManager(
            CoverStorageStrategy coverStrategy,
            CompatibleStorageStrategy compatibleStrategy,
            PerformanceStorageStrategy performanceStrategy) {
        this.storageStrategies = Map.of(
                StorageMode.COVER, coverStrategy,
                StorageMode.COMPATIBLE, compatibleStrategy,
                StorageMode.PERFORMANCE, performanceStrategy
        );
    }


    public StorageStrategy getStrategy(StorageMode mode) {
        return storageStrategies.get(mode);
    }

    public List<StorageStrategy> getAllStrategies() {
        return storageStrategies.values().stream().toList();
    }
}
