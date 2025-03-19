package top.nomelin.iot.model.alert;

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
    private Integer deviceId;//关联设备ID（与群组二选一）
    private Integer groupId;//关联群组ID（与设备二选一）,优先
    private Long createdTime;
    private Long updatedTime;

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
}
