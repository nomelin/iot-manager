package top.nomelin.iot.model.alert;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Alter
 *
 * @author nomelin
 * @since 2025/03/19 20:01
 **/
public class Alert {
    private Integer id;
    private Integer userId;
    private String name;
    private String description;
    private SimpleConditionConfig conditionConfig;//触发条件配置（JSON格式）
    private ActionConfig actionConfig;//触发后动作配置（JSON格式）
    private Boolean enable;
    private Integer deviceId;//关联设备ID，和群组ID至少有一个不为空
    private Integer groupId;//关联群组ID，和设备ID至少有一个不为空
    private Long createdTime;
    private Long updatedTime;

    public Alert(Integer id, Integer userId, String name, String description, SimpleConditionConfig conditionConfig,
                 ActionConfig actionConfig, Boolean enable, Integer deviceId,
                 Integer groupId, Long createdTime, Long updatedTime) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.conditionConfig = conditionConfig;
        this.actionConfig = actionConfig;
        this.enable = enable;
        this.deviceId = deviceId;
        this.groupId = groupId;
        this.createdTime = createdTime;
        this.updatedTime = updatedTime;
    }

    public Alert() {
    }

    @JsonIgnore
    public boolean isValid() {
        return (deviceId != null || groupId != null) && name != null && conditionConfig != null
                && actionConfig != null && conditionConfig.isValid() && actionConfig.isValid();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SimpleConditionConfig getConditionConfig() {
        return conditionConfig;
    }

    public void setConditionConfig(SimpleConditionConfig conditionConfig) {
        this.conditionConfig = conditionConfig;
    }

    public ActionConfig getActionConfig() {
        return actionConfig;
    }

    public void setActionConfig(ActionConfig actionConfig) {
        this.actionConfig = actionConfig;
    }

    public Boolean getEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public Long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Long createdTime) {
        this.createdTime = createdTime;
    }

    public Long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Long updatedTime) {
        this.updatedTime = updatedTime;
    }

    @Override
    public String toString() {
        return "Alert{" +
                "id=" + id +
                ", userId=" + userId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", conditionConfig=" + conditionConfig +
                ", actionConfig=" + actionConfig +
                ", enable=" + enable +
                ", deviceId=" + deviceId +
                ", groupId=" + groupId +
                ", createdTime=" + createdTime +
                ", updatedTime=" + updatedTime +
                '}';
    }
}
