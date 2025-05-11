package top.nomelin.iot.common;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * GlobalConfig
 *
 * @author nomelin
 * @since 2025/05/11
 **/
@Component
public class GlobalConfig {
    //TODO 待完善，配置分区，管理等。迁移现有配置。
    public static final Logger log = org.slf4j.LoggerFactory.getLogger(GlobalConfig.class);
    @Value("${iotdb.query.try-fast-aggregate}")
    private boolean tryFastAggregate;

    @PostConstruct
    public void init() {
        log.info("全局配置 initialized, tryFastAggregate配置为: " + tryFastAggregate);
    }

    public boolean isTryFastAggregate() {
        return tryFastAggregate;
    }

    public void setTryFastAggregate(boolean tryFastAggregate) {
        this.tryFastAggregate = tryFastAggregate;
    }

}
