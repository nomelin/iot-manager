package top.nomelin.iot.model;

import org.apache.iotdb.session.template.MeasurementNode;
import top.nomelin.iot.common.Constants;
import top.nomelin.iot.common.enums.IotDataType;

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
    private Map<String, IotDataType> dataTypes;

    public Map<String, IotDataType> getDataTypes() {
        return dataTypes;
    }

    public void setDataTypes(Map<String, IotDataType> dataTypes) {
        this.dataTypes = dataTypes;
    }

    @Override
    public String toString() {
        return "Config{" +
                "dataTypes=" + dataTypes +
                '}';
    }

    /**
     * 从配置信息解析出iotdb的MeasurementNode列表
     *
     * @return MeasurementNode列表
     */
    public List<MeasurementNode> convertMeasurementNodes() {
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
    public List<String> getMeasurements() {
        List<String> measurements = new ArrayList<>();
        for (Map.Entry<String, IotDataType> entry : this.getDataTypes().entrySet()) {
            measurements.add(entry.getKey());
        }
        return measurements;
    }
}
