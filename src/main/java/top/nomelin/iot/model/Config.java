package top.nomelin.iot.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.iotdb.session.template.MeasurementNode;
import top.nomelin.iot.common.Constants;
import top.nomelin.iot.model.enums.DeviceType;
import top.nomelin.iot.model.enums.IotDataType;
import top.nomelin.iot.model.enums.StorageMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 设备和模板的配置信息
 *
 * @author nomelin
 * @since 2024/12/27 15:41
 **/
public class Config {
    private Map<String, IotDataType> dataTypes;// 物理量名称和数据类型映射
    private String templateName;// 创建时使用的模板名称。模板config此项是null。
    private int aggregationTime;// 插入时聚合时间粒度
    private StorageMode storageMode;// 存储聚合模式。设备config和模板config此项有可能不同
    private DeviceType deviceType;// 设备类型

    /**
     * 从配置信息解析出iotdb的MeasurementNode列表
     *
     * @return MeasurementNode列表
     */
    public List<MeasurementNode> convertToMeasurementNodes() {
        List<MeasurementNode> measurementNodes = new ArrayList<>();
        for (Map.Entry<String, IotDataType> entry : this.getDataTypes().entrySet()) {
            MeasurementNode measurementNode = new MeasurementNode(
                    entry.getKey(), IotDataType.convertToTsDataType(entry.getValue()),
                    Constants.TS_ENCODING, Constants.COMPRESSION_TYPE);
            measurementNodes.add(measurementNode);
        }
        return measurementNodes;
    }

    /**
     * 获取所有物理量名称
     *
     * @return 所有物理量名称列表
     */
    @JsonIgnore
    public List<String> getMeasurements() {
        List<String> measurements = new ArrayList<>();
        for (Map.Entry<String, IotDataType> entry : this.getDataTypes().entrySet()) {
            measurements.add(entry.getKey());
        }
        return measurements;
    }


    public Map<String, IotDataType> getDataTypes() {
        return dataTypes;
    }

    public void setDataTypes(Map<String, IotDataType> dataTypes) {
        this.dataTypes = dataTypes;
    }


    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public int getAggregationTime() {
        return aggregationTime;
    }

    public void setAggregationTime(int aggregationTime) {
        this.aggregationTime = aggregationTime;
    }

    public StorageMode getStorageMode() {
        return storageMode;
    }

    public void setStorageMode(StorageMode storageMode) {
        this.storageMode = storageMode;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }

    @Override
    public String toString() {
        return "Config{" +
                "dataTypes=" + dataTypes +
                ", templateName='" + templateName + '\'' +
                ", aggregationTime=" + aggregationTime +
                ", StorageMode=" + storageMode +
                ", deviceType=" + deviceType +
                '}';
    }
}
