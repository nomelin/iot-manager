package top.nomelin.iot.dao;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import top.nomelin.iot.model.alert.ActionConfig;
import top.nomelin.iot.model.alert.Alert;
import top.nomelin.iot.model.alert.AlertChannel;
import top.nomelin.iot.model.alert.SimpleConditionConfig;
import top.nomelin.iot.model.enums.MessageType;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class AlertMapperTest {

    @Autowired
    private AlertMapper alertMapper;

    @Test
    void test() {
        // 插入1个告警
        Alert alert = new Alert();
        alert.setUserId(1);
        alert.setName("Test Alert测试告警");
        alert.setDescription("This is a test alert11测试");

        SimpleConditionConfig conditionConfig = new SimpleConditionConfig();
        conditionConfig.setDuration(60);
        conditionConfig.setMetric("temperature");
        conditionConfig.setMaxValue(30.0);
        alert.setConditionConfig(conditionConfig);

        List<Integer> notifyUsers = new ArrayList<>();
        notifyUsers.add(1);
        List<AlertChannel> channels = new ArrayList<>();
        AlertChannel channel = AlertChannel.IN_MSG;
        channels.add(channel);
        ActionConfig actionConfig = new ActionConfig(notifyUsers, channels, MessageType.WARNING, "Extra message额外");
        alert.setActionConfig(actionConfig);

        alert.setEnable(true);
        alert.setDeviceId(63);
        alert.setGroupId(null);
        long currentTime = System.currentTimeMillis();
        alert.setCreatedTime(currentTime);
        alert.setUpdatedTime(currentTime);

        alertMapper.insert(alert);
        Integer insertedId = alert.getId();
        System.out.println("插入的告警 ID: " + insertedId);

        // 按 id 查询
        Alert queriedAlert = alertMapper.selectById(insertedId);
        System.out.println("按 ID 查询的告警: " + queriedAlert);

        //更新
        queriedAlert.setName("Test Alert更新后的名称");
        alertMapper.update(queriedAlert);
        System.out.println("已更新 ID 为 " + insertedId + " 的告警");
        // 查询所有告警
        List<Alert> allAlerts = alertMapper.selectAll(null);
        System.out.println("查询所有告警数量: " + allAlerts.size());

        // 删除
        alertMapper.delete(insertedId);
        System.out.println("已删除 ID 为 " + insertedId + " 的告警");

        // 插入3个告警
        for (int i = 0; i < 3; i++) {
            Alert newAlert = new Alert();
            newAlert.setUserId(1);
            newAlert.setName("Test Alert " + i);
            newAlert.setDescription("This is test alert " + i);
            newAlert.setConditionConfig(conditionConfig);
            newAlert.setActionConfig(actionConfig);
            newAlert.setEnable(true);
            newAlert.setDeviceId(63);
            newAlert.setGroupId(null);
            newAlert.setCreatedTime(currentTime);
            newAlert.setUpdatedTime(currentTime);
            alertMapper.insert(newAlert);
            System.out.println("插入的告警 ID: " + newAlert.getId());
        }
        System.out.println("插入了3个告警");

        // 测试各个条件的查询
        Alert queryCondition = new Alert();
        queryCondition.setUserId(1);
        queryCondition.setName("Test Alert");
        List<Alert> alerts = alertMapper.selectAll(queryCondition);
        System.out.println("条件查询结果数量: " + alerts.size());
        for (Alert a : alerts) {
            System.out.println("条件查询结果: " + a);
        }
        System.out.println("条件查询结果数量: " + alerts.size());
    }
}