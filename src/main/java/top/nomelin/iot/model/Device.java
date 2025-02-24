package top.nomelin.iot.model;

import java.util.List;

/**
 * Device
 *
 * @author nomelin
 * @since 2024/12/20 20:16
 **/
public class Device {
    private Integer id;
    private String name;
    private Integer userId;
    private Integer templateId;
    private List<String> tags;

    private List<Integer> groupIds;//这个字段不在device表中，而是通过中间表group_device关联。查表时通过left join group_device来获取。

    private Config config;

    public Integer getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }


    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }


    public Integer getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
    }

    public List<Integer> getGroupIds() {
        return groupIds;
    }

    public void setGroupIds(List<Integer> groupIds) {
        this.groupIds = groupIds;
    }


    @Override
    public String toString() {
        return "Device{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", userId=" + userId +
                ", templateId=" + templateId +
                ", tags=" + tags +
                ", groupIds=" + groupIds +
                ", config='" + config + '\'' +
                '}';
    }

}
