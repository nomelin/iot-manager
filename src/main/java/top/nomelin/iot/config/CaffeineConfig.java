package top.nomelin.iot.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * CaffeineConfig
 *
 * @author nomelin
 * @since 2025/03/13 11:55
 **/
@Configuration
//@EnableCaching
public class CaffeineConfig {
    /**
     * 自定义Caffeine配置
     */
//    @Bean
//    public Cache<Object, Object> caffeineConfig() {
//        return Caffeine.newBuilder()
//                .initialCapacity(128)
//                .maximumSize(1024)
//                .expireAfterWrite(600, TimeUnit.MINUTES)
//                .recordStats()
//                .build();
//    }
}

