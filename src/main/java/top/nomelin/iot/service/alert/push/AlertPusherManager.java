package top.nomelin.iot.service.alert.push;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.model.alert.AlertChannel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AlertPusherManager
 *
 * @author nomelin
 * @since 2025/03/25
 **/
@Component
public class AlertPusherManager {
    private final Map<AlertChannel, AlertPushStrategy> pushStrategies = new ConcurrentHashMap<>();

    @Autowired
    public AlertPusherManager(List<AlertPushStrategy> pushStrategies) {
        pushStrategies.forEach(strategy -> this.pushStrategies.put(strategy.getChannel(), strategy));
    }

    public AlertPushStrategy getPushStrategy(AlertChannel channel) {
        if (!pushStrategies.containsKey(channel)) {
            throw new BusinessException(CodeMessage.UNSUPPORTED_ALERT_CHANNEL_ERROR, "不支持的告警渠道：" + channel.name());
        }
        return pushStrategies.get(channel);
    }
}
