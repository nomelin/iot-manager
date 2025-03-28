package top.nomelin.iot.model.enums;

/**
 * QueryAggregateFunc
 *
 * @author nomelin
 * @since 2025/01/27 17:23
 **/
public enum QueryAggregateFunc {
    AVG("平均", "对聚合时间粒度内的数值取平均值。若为字符串类型(包括数据行标识)，则等同于FIRST"),
    MAX("最大值", "对聚合时间粒度内的数值取最大值。若为字符串类型(包括数据行标识)，则取最长的字符串"),
    MIN("最小值", "对聚合时间粒度内的数值取最小值。若为字符串类型(包括数据行标识)，则取最短的字符串"),
    SUM("总和", "对聚合时间粒度内的数值求和。若为字符串类型(包括数据行标识)，则等同于COUNT"),
    COUNT("计数", "对聚合时间粒度内的数值计数"),
    FIRST("最旧值", "取聚合时间粒度内的最旧数值"),
    LAST("最新值", "取聚合时间粒度内的最新数值");
    private final String name;
    private final String desc;

    QueryAggregateFunc(String name, String desc) {
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
