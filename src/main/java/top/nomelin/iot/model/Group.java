package top.nomelin.iot.model;

import java.util.List;

/**
 * Group
 *
 * @author nomelin
 * @since 2024/12/20 20:21
 **/
public class Group {
    private Integer id;
    private String name;
    private String description;
    private Integer userId;
    private List<Integer> deviceIds;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public List<Integer> getDeviceIds() {
        return deviceIds;
    }

    public void setDeviceIds(List<Integer> deviceIds) {
        this.deviceIds = deviceIds;
    }

    @Override
    public String toString() {
        return "Group{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", userId=" + userId +
                ", deviceIds=" + deviceIds +
                '}';
    }
}
