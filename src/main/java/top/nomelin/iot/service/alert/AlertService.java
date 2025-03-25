package top.nomelin.iot.service.alert;

import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.alert.Alert;

import java.util.List;
import java.util.Map;

/**
 * 告警服务
 *
 * @author nomelin
 * @since 2025/03/25
 **/
public interface AlertService {
    Alert checkPermission(int alertId);

    // 配置管理
    Alert createAlert(Alert alert);

    Alert updateAlert(Alert alert);

    Alert getAlert(int id);

    List<Alert> listAlerts(Alert query);

    List<Alert> getAllAlerts();

    void deleteAlert(int id);

    /**
     * 处理设备数据并触发告警检查
     *
     * @param deviceId 设备ID，和device参数二选一
     * @param device   设备信息，和deviceId参数二选一。如果传递此参数，则会少查一次设备表。
     * @param metrics  设备指标数据
     */
    void processDeviceData(Integer deviceId, Device device, Map<String, Object> metrics);

    /**
     * 获取设备相关的所有告警配置（直接关联+群组关联）
     */
    List<Alert> getAlertsByDevice(Device device);
}
