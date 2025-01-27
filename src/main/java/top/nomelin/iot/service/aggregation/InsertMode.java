package top.nomelin.iot.service.aggregation;

/**
 * InsertMode
 * //TODO 改成StorageMode。因为是存储，而且存取是成对的，类似于编码和解码。
 *
 * @author nomelin
 * @since 2025/01/27 17:12
 **/
public enum InsertMode {
    COVER("覆盖", "新数据会覆盖旧数据，旧数据会永久丢失，请注意"),
    COMPATIBLE("聚合存储-兼容", "支持任意时间粒度数据聚合，不限制聚合数量。查询性能相比较低，存储空间占用较大"),
    PERFORMANCE("聚合存储-性能", "仅支持秒级以上数据聚合，每秒最多聚合1000条数据。有较高的查询性能和存储压缩比");
    private final String name;
    private final String desc;

    InsertMode(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }
}
