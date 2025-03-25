package top.nomelin.iot.service.alert.impl;

import cn.hutool.core.util.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.nomelin.iot.cache.CurrentUserCache;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.dao.AlertMapper;
import top.nomelin.iot.model.Device;
import top.nomelin.iot.model.alert.ActionConfig;
import top.nomelin.iot.model.alert.Alert;
import top.nomelin.iot.model.alert.AlertChannel;
import top.nomelin.iot.service.DeviceService;
import top.nomelin.iot.service.alert.AlertService;
import top.nomelin.iot.service.alert.push.AlertPushStrategy;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlertServiceImpl implements AlertService {
    public static final Logger log = LoggerFactory.getLogger(AlertServiceImpl.class);
    private final AlertMapper alertMapper;
    private final DeviceService deviceService;
    private final Map<AlertChannel, AlertPushStrategy> pushStrategies = new ConcurrentHashMap<>();
    private final CurrentUserCache currentUserCache;//TODO 清理机制


    // 记录告警状态：设备ID + 告警ID -> 告警状态。每个设备对应每个告警配置都单独计时。
    private final Map<String, AlertState> alertStates = new ConcurrentHashMap<>();

    public AlertServiceImpl(AlertMapper alertMapper, DeviceService deviceService, CurrentUserCache currentUserCache) {
        this.alertMapper = alertMapper;
        this.deviceService = deviceService;
        this.currentUserCache = currentUserCache;
    }

    @Override
    public Alert checkPermission(int alertId) {
        Alert alert = alertMapper.selectById(alertId);
        if (ObjectUtil.isNull(alert)) {
            log.warn("告警配置不存在, alertId: {}", alertId);
            throw new BusinessException(CodeMessage.NOT_FOUND_ERROR);
        }
        if (!alert.getUserId().equals(currentUserCache.getCurrentUser().getId())) {
            log.warn("非法操作, 非告警配置拥有者操作 alertId: {}, userId: {}", alertId, currentUserCache.getCurrentUser().getId());
            throw new BusinessException(CodeMessage.NO_PERMISSION_ERROR);
        }
        return alert;
    }

    @Override
    public Alert createAlert(Alert alert) {
        alert.setCreatedTime(System.currentTimeMillis());
        alert.setUpdatedTime(System.currentTimeMillis());
        alert.setUserId(currentUserCache.getCurrentUser().getId());
        alert.setEnable(true);
        alertMapper.insert(alert);
        log.info("创建告警配置成功: {}", alert);
        return alert;
    }

    @Override
    public Alert updateAlert(Alert alert) {
        checkPermission(alert.getId());
        alert.setUpdatedTime(System.currentTimeMillis());
        alertMapper.update(alert);
        log.info("更新告警配置成功: {}", alert);
        return alert;
    }

    @Override
    public Alert getAlert(int id) {
        return checkPermission(id);
    }

    @Override
    public List<Alert> listAlerts(Alert query) {
        query.setUserId(currentUserCache.getCurrentUser().getId());
        return alertMapper.selectAll(query);
    }

    @Override
    public List<Alert> getAllAlerts() {
        Alert query = new Alert();
        query.setUserId(currentUserCache.getCurrentUser().getId());
        return alertMapper.selectAll(query);
    }


    @Override
    public void deleteAlert(int id) {
        checkPermission(id);
        alertMapper.delete(id);
        log.info("删除告警配置成功: id: {}", id);
    }

    @Override
    public void processDeviceData(int deviceId, Device device, Map<String, Object> metrics) {
        List<Alert> deviceAlerts;
        if (ObjectUtil.isNotNull(deviceId)) {
            device = deviceService.getDeviceById(deviceId);
            deviceAlerts = getAlertsByDevice(device);
        } else if (ObjectUtil.isNotNull(device)) {
            deviceAlerts = getAlertsByDevice(device);
        } else {
            log.warn("判断告警配置时未指定设备或设备ID");
            return;
        }
        //遍历所有告警配置
        for (Alert alert : deviceAlerts) {
            if (!alert.getEnable()) {
                continue;
            }
            String stateKey = deviceId + "-" + alert.getId();
            AlertState state = alertStates.computeIfAbsent(stateKey, k -> new AlertState());
            // 检查静默期
            if (isInSilentPeriod(alert.getActionConfig(), state)) {
                continue;
            }
            // 评估触发条件
            if (alert.getConditionConfig().evaluate(metrics)) {
                handleTriggeredAlert(alert, device, metrics, state);
            } else {
                state.resetStartTime();//条件不满足，重置开始时间。
            }
        }
    }

    /**
     * 获取设备相关的所有告警配置（直接关联+群组关联）
     */
    private List<Alert> getAlertsByDevice(Device device) {
        // 获取直接关联设备的告警
        Alert query = new Alert();
        query.setDeviceId(device.getId());
        List<Alert> alerts = alertMapper.selectAll(query);

        // 获取群组关联的告警
        List<Integer> groupIds = device.getGroupIds();
        groupIds.forEach(groupId -> {
            query.setDeviceId(null);
            query.setGroupId(groupId);
            alerts.addAll(alertMapper.selectAll(query));
        });

        return alerts;
    }

    // 判断是否在静默期
    private boolean isInSilentPeriod(ActionConfig actionConfig, AlertState state) {
        // 没有静默期
        if (actionConfig.getSilentDuration() == null || actionConfig.getSilentDuration() <= 0) {
            return false;
        }
        return System.currentTimeMillis() - state.getLastTriggerTime()
                < actionConfig.getSilentDuration() * 1000L;
    }

    // 处理触发的告警
    private void handleTriggeredAlert(Alert alert, Device device,
                                      Map<String, Object> metrics, AlertState state) {
        // 检查持续时间要求
        long currentTime = System.currentTimeMillis();
        if (state.getStartTime() == null) {
            state.setStartTime(currentTime);
        }

        long duration = (currentTime - state.getStartTime()) / 1000;
        if (duration >= alert.getConditionConfig().getDuration()) {
            triggerActions(alert, device, metrics);
            state.updateLastTrigger(currentTime);// 更新上次触发时间
//            state.resetStartTime();
        }
    }

    private void triggerActions(Alert alert, Device device, Map<String, Object> triggerValue) {
        ActionConfig actionConfig = alert.getActionConfig();
        for (AlertChannel channel : actionConfig.getChannels()) {
            AlertPushStrategy strategy = pushStrategies.get(channel);
            if (strategy != null) {
                strategy.push(alert, triggerValue, device);
            }
        }
        log.info("触发告警动作: id：{}, 设备Id: {}, 触发值: {}", alert.getId(), device.getId(), triggerValue);
    }

    @Override
    public void registerPushStrategy(AlertChannel channel, AlertPushStrategy strategy) {
        pushStrategies.put(channel, strategy);
    }

    // 内部状态跟踪类，用来计算告警持续时间和告警推送静默时间
    private static class AlertState {
        private Long startTime;// 告警开始时间
        private Long lastTriggerTime;// 上次触发推送时间

        public Long getStartTime() {
            return startTime;
        }

        public void setStartTime(Long startTime) {
            this.startTime = startTime;
        }

        public Long getLastTriggerTime() {
            return lastTriggerTime;
        }

        public void updateLastTrigger(Long time) {
            this.lastTriggerTime = time;
        }

        public void resetStartTime() {
            this.startTime = null;
        }
    }
}