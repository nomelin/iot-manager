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
    @Value("${iotdb.query.try-fast-aggregate:true}")
    private boolean tryFastAggregate;
    @Value("${iotdb.query.fast-aggregate-error-send-message:false}")
    private boolean fastAggregateErrorMessage;

    @Value("${iotdb.query.aggregate-query-limit-size:10000000}")
    private int aggregateQueryLimitSize;

    @PostConstruct
    public void init() {
        log.info("全局配置 initialized");
        log.info("tryFastAggregate: " + tryFastAggregate);
        log.info("fastAggregateErrorMessage: " + fastAggregateErrorMessage);
        log.info("aggregateQueryLimitSize: " + aggregateQueryLimitSize);
    }

    public boolean isTryFastAggregate() {
        return tryFastAggregate;
    }

    public void setTryFastAggregate(boolean tryFastAggregate) {
        this.tryFastAggregate = tryFastAggregate;
    }

    public boolean isFastAggregateErrorMessage() {
        return fastAggregateErrorMessage;
    }

    public void setFastAggregateErrorMessage(boolean fastAggregateErrorMessage) {
        this.fastAggregateErrorMessage = fastAggregateErrorMessage;
    }

    public int getAggregateQueryLimitSize() {
        return aggregateQueryLimitSize;
    }

    public void setAggregateQueryLimitSize(int aggregateQueryLimitSize) {
        this.aggregateQueryLimitSize = aggregateQueryLimitSize;
    }


}
