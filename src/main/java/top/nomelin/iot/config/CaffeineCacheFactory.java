package top.nomelin.iot.config;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class CaffeineCacheFactory<K, V> implements FactoryBean<Cache<K, V>> {

    @Override
    public Cache<K, V> getObject() {
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .recordStats()
                .build();
    }

    @Override
    public Class<?> getObjectType() {
        return Cache.class;
    }
}
