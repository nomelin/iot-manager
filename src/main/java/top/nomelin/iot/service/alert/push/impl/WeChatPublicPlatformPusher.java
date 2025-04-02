package top.nomelin.iot.service.alert.push.impl;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.User;
import top.nomelin.iot.model.alert.ActionConfig;
import top.nomelin.iot.model.alert.Alert;
import top.nomelin.iot.model.alert.AlertChannel;
import top.nomelin.iot.model.alert.SimpleConditionConfig;
import top.nomelin.iot.service.UserService;
import top.nomelin.iot.service.alert.push.AlertPushStrategy;
import top.nomelin.iot.service.alert.push.WechatPushService;
import top.nomelin.iot.util.TimeUtil;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * WeChatPublicPlatformPusher
 *
 * @author nomelin
 * @since 2025/03/25
 **/
@Component
public class WeChatPublicPlatformPusher implements AlertPushStrategy {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(WeChatPublicPlatformPusher.class);

    private final WechatPushService wechatPushService;

    private final UserService userService;

    public WeChatPublicPlatformPusher(WechatPushService wechatPushService, UserService userService) {
        this.wechatPushService = wechatPushService;
        this.userService = userService;
    }

    @Override
    public void push(Alert alert, Map<String, Object> triggerValue, Device device, Long triggerTime) {
        ActionConfig actionConfig = alert.getActionConfig();
        SimpleConditionConfig conditionConfig = alert.getConditionConfig();
        Object value = triggerValue.get(conditionConfig.getMetric());
        User user = userService.selectById(alert.getUserId());
        if (Objects.isNull(user) || StringUtils.isEmpty(user.getPushplusToken())) {
            log.warn("用户({})不存在或未配置pushplusToken, 无法进行微信公众号推送", alert.getUserId());
            return;
        }
        //时间戳格式化
        String time = TimeUtil.timestampToDateStringS(triggerTime);
        String title = String.format("设备:(%s)[%s] 传感器(%s)告警,当前值:%s",
                device.getId(), device.getName(), conditionConfig.getMetric(), value.toString());
        String message = String.format("用户ID: %s<br>" +
                        "设备ID: %s, 设备名称: %s<br>" +
                        "传感器: %s 触发告警，请及时处理。当前值: %s ,超过配置告警阈值: %s ~ %s ，持续时间 > %s秒<br>" +
                        "告警触发时间:%s，<br>" +
                        "此告警触发后静默时间: %s 秒<br>" +
                        "来自告警规则ID: %s, 名称: %s <br>" +
                        "额外信息: %s",
                alert.getUserId(), device.getId(), device.getName(), conditionConfig.getMetric(), value,
                conditionConfig.getMinValue(), conditionConfig.getMaxValue(), conditionConfig.getDuration(), time,
                actionConfig.getSilentDuration(), alert.getId(), alert.getName(), actionConfig.getExtraMessage());
        log.info("微信公众号推送消息: " + message);
        wechatPushService.send(title, message, alert.getUserId(), List.of(user.getPushplusToken()));
    }

    @Override
    public AlertChannel getChannel() {
        return AlertChannel.WECHAT_PP;
    }
}
