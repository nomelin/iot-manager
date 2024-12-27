package top.nomelin.iot.model;

/**
 * Template
 *
 * @author nomelin
 * @since 2024/12/27 15:39
 **/
public class Template {
    private Integer id;
    private String name;
    private Integer userId;
    private Config config;

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

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    @Override
    public String toString() {
        return "Template{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", userId=" + userId +
                ", config='" + config + '\'' +
                '}';
    }
}
