package top.nomelin.iot.controller.request;

import java.util.List;

public class DataInsertRequest {
    private Integer deviceId;//必须
    private List<Long> timestamps;//必须
    private List<String> measurements;//必须
    private List<List<Object>> values;//必须

    private Integer mergeTimestampNum;//可选，为null时默认全量合并

    private String tag;//可选，为null时默认无标签

    //可选，为null时不划分批次，直接插入，或者说批次为无限。
    // 兼容模式和性能模式下如果一次插入的数据过多，可能会失败，建议设置，例如1000。COVER模式一般不必要。
    private Integer batchSize;

    // Getters and Setters
    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public List<Long> getTimestamps() {
        return timestamps;
    }

    public void setTimestamps(List<Long> timestamps) {
        this.timestamps = timestamps;
    }

    public List<String> getMeasurements() {
        return measurements;
    }

    public void setMeasurements(List<String> measurements) {
        this.measurements = measurements;
    }

    public List<List<Object>> getValues() {
        return values;
    }

    public void setValues(List<List<Object>> values) {
        this.values = values;
    }


    public Integer getMergeTimestampNum() {
        return mergeTimestampNum;
    }

    public void setMergeTimestampNum(Integer mergeTimestampNum) {
        this.mergeTimestampNum = mergeTimestampNum;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }


    @Override
    public String toString() {
        return "DataInsertRequest{" +
                "deviceId=" + deviceId +
                ", timestamps=" + timestamps +
                ", measurements=" + measurements +
                ", values=" + values +
                ", mergeTimestampNum=" + mergeTimestampNum +
                ", tag='" + tag + '\'' +
                ", batchSize=" + batchSize +
                '}';
    }
}
