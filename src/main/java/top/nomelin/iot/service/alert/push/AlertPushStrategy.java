package top.nomelin.iot.service.alert.push;

import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.alert.Alert;

import java.util.Map;

/**
 * 告警推送策略
 *
 * @author nomelin
 * @since 2025/03/25
 **/
public interface AlertPushStrategy {
    /**
     * 推送告警通知
     *
     * @param alert        告警配置，包含告警动作配置
     * @param triggerValue 触发告警的键值对
     * @param device       告警设备
     */
    void push(Alert alert, Map<String, Object> triggerValue, Device device);
}
