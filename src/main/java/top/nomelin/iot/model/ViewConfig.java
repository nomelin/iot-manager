package top.nomelin.iot.model;

import top.nomelin.iot.service.aggregation.QueryMode;

/**
 * ViewConfig
 *
 * @author nomelin
 * @since 2025/01/26 23:16
 **/
public class ViewConfig {
    private String name;
    private int aggregationTime;// 查询时聚合时间粒度
    private QueryMode queryMode;// 查询模式


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAggregationTime() {
        return aggregationTime;
    }

    public void setAggregationTime(int aggregationTime) {
        this.aggregationTime = aggregationTime;
    }

    public QueryMode getQueryMode() {
        return queryMode;
    }

    public void setQueryMode(QueryMode queryMode) {
        this.queryMode = queryMode;
    }

    @Override
    public String toString() {
        return "ViewConfig{" +
                "name='" + name + '\'' +
                ", aggregationTime=" + aggregationTime +
                ", queryMode=" + queryMode +
                '}';
    }
}
