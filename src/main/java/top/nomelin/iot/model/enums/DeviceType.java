package top.nomelin.iot.model.enums;

/**
 * DeviceType
 *
 * @author nomelin
 * @since 2025/03/25
 **/
public enum DeviceType {
    DEVICE("设备"),
    DATASET("数据集");
    private final String name;

    DeviceType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
