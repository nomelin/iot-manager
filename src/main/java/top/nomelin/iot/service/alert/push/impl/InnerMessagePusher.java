package top.nomelin.iot.service.alert.push.impl;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.alert.ActionConfig;
import top.nomelin.iot.model.alert.Alert;
import top.nomelin.iot.model.alert.AlertChannel;
import top.nomelin.iot.model.alert.SimpleConditionConfig;
import top.nomelin.iot.service.MessageService;
import top.nomelin.iot.service.alert.push.AlertPushStrategy;
import top.nomelin.iot.util.TimeUtil;

import java.util.Map;

/**
 * 站内信推送
 *
 * @author nomelin
 * @since 2025/03/25
 **/
@Component
public class InnerMessagePusher implements AlertPushStrategy {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(InnerMessagePusher.class);

    private final MessageService messageService;


    public InnerMessagePusher(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void push(Alert alert, Map<String, Object> triggerValue, Device device, Long triggerTime) {
        ActionConfig actionConfig = alert.getActionConfig();
        SimpleConditionConfig conditionConfig = alert.getConditionConfig();
        Object value = triggerValue.get(conditionConfig.getMetric());
        //时间戳格式化
        String time = TimeUtil.timestampToDateStringS(triggerTime);
        String title = String.format("设备:(%s)[%s]-传感器(%s)告警,当前值:%s",
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
        log.info("站内信推送消息: " + message);
        for (Integer userId : actionConfig.getNotifyUsers()) {
            messageService.sendSystemMessage(userId, title, message, actionConfig.getMessageType());
        }
    }

    @Override
    public AlertChannel getChannel() {
        return AlertChannel.IN_MSG;
    }
}
