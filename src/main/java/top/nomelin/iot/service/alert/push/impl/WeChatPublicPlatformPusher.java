package top.nomelin.iot.service.alert.push.impl;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.alert.Alert;
import top.nomelin.iot.model.alert.AlertChannel;
import top.nomelin.iot.service.alert.push.AlertPushStrategy;

import java.util.Map;

/**
 * WeChatPublicPlatformPusher
 *
 * @author nomelin
 * @since 2025/03/25
 **/
@Component
public class WeChatPublicPlatformPusher implements AlertPushStrategy {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(WeChatPublicPlatformPusher.class);

    @Override
    public void push(Alert alert, Map<String, Object> triggerValue, Device device, Long triggerTime) {
        log.warn("微信公众号推送功能还未实现.");
    }

    @Override
    public AlertChannel getChannel() {
        return AlertChannel.WECHAT_PP;
    }
}
