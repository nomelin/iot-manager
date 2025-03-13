package top.nomelin.iot.cache.impl;

import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import top.nomelin.iot.cache.CacheOperations;
import top.nomelin.iot.cache.CacheResult;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caffeine缓存实现
 */
@Component
class CaffeineCacheOperations<K, V> implements CacheOperations<K, V> {
    private static final Logger log = LoggerFactory.getLogger(CaffeineCacheOperations.class);

    private final Cache<K, V> cache;

    public CaffeineCacheOperations(Cache<K, V> cache) {
        this.cache = cache;
    }

    @Override
    public CacheResult<V> get(@NonNull K key) {
        try {
            V value = cache.getIfPresent(key);
            if (value != null) {
//                log.info("Cache hit - Key: {}", key);
                return CacheResult.hit(value);
            }
            log.info("Cache miss - Key: {}", key);
            return CacheResult.miss();
        } catch (Exception e) {
            log.error("Cache get error - Key: {}", key, e);
            return CacheResult.failure(e);
        }
    }

    @Override
    public Map<K, V> getAll(@NonNull Set<K> keys) {
        try {
            Map<K, V> result = cache.getAllPresent(keys);
//            log.info("Batch get - Request keys: {}, Found keys: {}", keys.size(), result.size());
            return result;
        } catch (Exception e) {
            log.error("Batch get error - Keys: {}", keys, e);
            return Collections.emptyMap();
        }
    }

    @Override
    public void put(@NonNull K key, V value) {
        try {
            cache.put(key, value);
            log.info("Cache put - Key: {}", key);
        } catch (Exception e) {
            log.error("Cache put error - Key: {}", key, e);
        }
    }

    @Override
    public void put(@NonNull K key, V value, Duration duration) {
        try {
            // Caffeine需要提前配置过期策略，这里只是示例实现
            log.error("Caffeine cache 不支持设置过期时间，请使用put(K, V)方法");
//            log.info("Cache put with TTL - Key: {}, TTL: {}", key, duration);
        } catch (Exception e) {
            log.error("Cache put with TTL error - Key: {}", key, e);
        }
    }

    @Override
    public void putAll(@NonNull Map<K, V> entries) {
        try {
            cache.putAll(entries);
            log.info("Batch put - Entries: {}", entries.size());
        } catch (Exception e) {
            log.error("Batch put error", e);
        }
    }

    @Override
    public boolean remove(@NonNull K key) {
        try {
            cache.invalidate(key);
            log.info("Cache remove - Key: {}", key);
            return true;
        } catch (Exception e) {
            log.error("Cache remove error - Key: {}", key, e);
            return false;
        }
    }

    @Override
    public void removeAll(@NonNull Set<K> keys) {
        try {
            cache.invalidateAll(keys);
            log.info("Batch remove - Keys: {}", keys.size());
        } catch (Exception e) {
            log.error("Batch remove error", e);
        }
    }

    @Override
    public boolean containsKey(@NonNull K key) {
        try {
            return cache.getIfPresent(key) != null;
        } catch (Exception e) {
            log.error("Contains key error - Key: {}", key, e);
            return false;
        }
    }

    @Override
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        try {
            com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats = cache.stats();
            stats.put("hitCount", caffeineStats.hitCount());
            stats.put("missCount", caffeineStats.missCount());
            stats.put("loadSuccessCount", caffeineStats.loadSuccessCount());
            stats.put("loadFailureCount", caffeineStats.loadFailureCount());
            stats.put("totalLoadTime", caffeineStats.totalLoadTime());
            stats.put("evictionCount", caffeineStats.evictionCount());
        } catch (Exception e) {
            log.error("Get stats error", e);
        }
        return stats;
    }

    @Override
    public void clear() {
        try {
            cache.invalidateAll();
            log.info("Cache cleared");
        } catch (Exception e) {
            log.error("Clear cache error", e);
        }
    }
}
