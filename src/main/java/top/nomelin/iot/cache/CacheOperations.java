package top.nomelin.iot.cache;


import org.springframework.lang.NonNull;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

/**
 * 通用缓存操作接口
 *
 * @param <K> 键类型
 * @param <V> 值类型
 */
public interface CacheOperations<K, V> {

    /**
     * 获取缓存值
     *
     * @param key 缓存键
     * @return 缓存结果包装对象
     */
    CacheResult<V> get(@NonNull K key);

    /**
     * 批量获取缓存值
     *
     * @param keys 缓存键集合
     * @return 包含命中结果的Map，未命中的键不会出现在Map中
     */
    Map<K, V> getAll(@NonNull Set<K> keys);

    /**
     * 放入缓存（永不过期）
     *
     * @param key   缓存键
     * @param value 缓存值
     */
    void put(@NonNull K key, V value);

    /**
     * 带过期时间的缓存放入
     *
     * @param key      缓存键
     * @param value    缓存值
     * @param duration 过期时间
     */
    void put(@NonNull K key, V value, Duration duration);

    /**
     * 批量放入缓存
     *
     * @param entries 键值对集合
     */
    void putAll(@NonNull Map<K, V> entries);

    /**
     * 删除缓存
     *
     * @param key 缓存键
     * @return 是否删除成功
     */
    boolean remove(@NonNull K key);

    /**
     * 批量删除缓存
     *
     * @param keys 缓存键集合
     */
    void removeAll(@NonNull Set<K> keys);

    /**
     * 判断缓存是否存在
     *
     * @param key 缓存键
     * @return 是否存在
     */
    boolean containsKey(@NonNull K key);

    /**
     * 获取缓存统计信息
     *
     * @return 统计信息Map
     */
    Map<String, Object> getStats();

    /**
     * 清空所有缓存
     */
    void clear();

    /**
     * 安全获取缓存值（带降级保护）
     *
     * @param key          缓存键
     * @param defaultValue 降级默认值
     * @return 缓存值或默认值
     */
    default V getOrDefault(K key, V defaultValue) {
        CacheResult<V> result = get(key);
        if (result.isSuccess() && result.getData() != null) {
            return result.getData();
        }
        return defaultValue;
    }
}

