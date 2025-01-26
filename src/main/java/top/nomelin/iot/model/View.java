package top.nomelin.iot.model;

/**
 * View
 *
 * @author nomelin
 * @since 2025/01/26 23:15
 **/
public class View {
    private Integer id;
    private String measurement;
    private Integer groupId;
    private ViewConfig config;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMeasurement() {
        return measurement;
    }

    public void setMeasurement(String measurement) {
        this.measurement = measurement;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public ViewConfig getConfig() {
        return config;
    }

    public void setConfig(ViewConfig config) {
        this.config = config;
    }

    @Override
    public String toString() {
        return "View{" +
                "id=" + id +
                ", measurement='" + measurement + '\'' +
                ", groupId=" + groupId +
                ", config=" + config +
                '}';
    }
}
