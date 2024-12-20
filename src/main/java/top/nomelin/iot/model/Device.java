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
    private List<String> tags;

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


    @Override
    public String toString() {
        return "Device{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", userId=" + userId +
                ", tags=" + tags +
                '}';
    }
}
