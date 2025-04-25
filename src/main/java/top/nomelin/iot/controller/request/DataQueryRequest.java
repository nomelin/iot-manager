package top.nomelin.iot.controller.request;

import top.nomelin.iot.model.enums.QueryAggregateFunc;

import java.util.List;

public class DataQueryRequest {
    private Integer deviceId;//必须
    private Long startTime;//可选，为null代表不限制
    private Long endTime;//可选，为null代表不限制
    private List<String> selectMeasurements;//可选，为null代表查询全部
    private Integer aggregationTime;//可选，为null代表不聚合
    private QueryAggregateFunc queryAggregateFunc;//可选，为null代表不聚合
    private List<List<Double>> thresholds;//可选，为null代表不阈值过滤

    private String tagQuery;//可选，为null代表不标签过滤

    // Getters and Setters
    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public List<String> getSelectMeasurements() {
        return selectMeasurements;
    }

    public void setSelectMeasurements(List<String> selectMeasurements) {
        this.selectMeasurements = selectMeasurements;
    }

    public Integer getAggregationTime() {
        return aggregationTime;
    }

    public void setAggregationTime(Integer aggregationTime) {
        this.aggregationTime = aggregationTime;
    }

    public QueryAggregateFunc getQueryAggregateFunc() {
        return queryAggregateFunc;
    }

    public void setQueryAggregateFunc(QueryAggregateFunc queryAggregateFunc) {
        this.queryAggregateFunc = queryAggregateFunc;
    }

    public List<List<Double>> getThresholds() {
        return thresholds;
    }

    public void setThresholds(List<List<Double>> thresholds) {
        this.thresholds = thresholds;
    }

    public String getTagQuery() {
        return tagQuery;
    }

    public void setTagQuery(String tagQuery) {
        this.tagQuery = tagQuery;
    }

    @Override
    public String toString() {
        return "DataQueryRequest{" +
                "deviceId=" + deviceId +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", selectMeasurements=" + selectMeasurements +
                ", aggregationTime=" + aggregationTime +
                ", queryAggregateFunc=" + queryAggregateFunc +
                ", thresholds=" + thresholds +
                ", tagQuery='" + tagQuery + '\'' +
                '}';
    }
}