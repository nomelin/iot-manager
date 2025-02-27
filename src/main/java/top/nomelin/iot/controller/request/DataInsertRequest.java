package top.nomelin.iot.controller.request;

import java.util.List;

public class DataInsertRequest {
    private Integer deviceId;
    private List<Long> timestamps;
    private List<String> measurements;
    private List<List<Object>> values;

    private Integer mergeTimestampNum;

    private String tag;

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

    @Override
    public String toString() {
        return "DataInsertRequest{" +
                "deviceId=" + deviceId +
                ", timestamps=" + timestamps +
                ", measurements=" + measurements +
                ", values=" + values +
                ", mergeTimestampNum=" + mergeTimestampNum +
                ", tag='" + tag + '\'' +
                '}';
    }
}
