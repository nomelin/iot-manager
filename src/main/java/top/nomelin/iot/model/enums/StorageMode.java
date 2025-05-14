package top.nomelin.iot.model.enums;

/**
 * StorageMode
 *
 * @author nomelin
 * @since 2025/01/27 17:12
 **/
public enum StorageMode {
    COVER("覆盖(推荐)", "相同时间戳的新数据会覆盖旧数据。所有性能均最为优秀，推荐使用。支持[极速聚合]"),
    COMPATIBLE("合并-兼容", "支持任意时间粒度数据并存，不限制聚合数量。存储性能，查询性能，" +
            "空间性能均略差于覆盖模式。[不]支持[极速聚合]"),
    PERFORMANCE("合并-性能", "仅支持秒级以上数据并存，每秒最多聚合1000条数据。" +
            "存储性能很差，但是查询性能，空间性能和覆盖模式相当。支持[极速聚合]");
    private final String name;
    private final String desc;

    StorageMode(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }
}
