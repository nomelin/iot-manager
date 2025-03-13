package top.nomelin.iot.cache;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.Set;

@SpringBootTest
class CacheOperationsTest {

    @Autowired
    private CacheOperations<String, String> cacheOperations;

    @Test
    void testAll() {
        String key1 = "key1";
        String key2 = "key2";
        String key3 = "key3";
        String value1 = "value1";
        String value2 = "value2";

        // 测试 put
        cacheOperations.put(key1, value1);
        cacheOperations.put(key2, value2);

        // 测试 get
        System.out.println("Get key1: " + cacheOperations.get(key1));
        System.out.println("Get key2: " + cacheOperations.get(key2));
        System.out.println("Get key3 (不存在): " + cacheOperations.get(key3));

        // 测试 getAll
        System.out.println("Get all: " + cacheOperations.getAll(Set.of(key1, key2, key3)));

        // 测试 containsKey
        System.out.println("Contains key1: " + cacheOperations.containsKey(key1));
        System.out.println("Contains key3: " + cacheOperations.containsKey(key3));

        // 测试 remove
        System.out.println("Remove key1: " + cacheOperations.remove(key1));
        System.out.println("Get key1 after remove: " + cacheOperations.get(key1));

        // 测试 putAll
        cacheOperations.putAll(Map.of("keyA", "valueA", "keyB", "valueB"));
        System.out.println("Get all after putAll: " + cacheOperations.getAll(Set.of("key2", "keyA", "keyB")));

        // 测试 getStats
        System.out.println("Cache stats: " + cacheOperations.getStats());

        System.out.println("All cache keys: " + cacheOperations.getAllKeys());

        // 最后清空缓存
        cacheOperations.clear();
        System.out.println("Cache cleared.");
    }
}
