package top.nomelin.iot.common.enums;

import org.apache.tsfile.enums.TSDataType;

/**
 * IotDataType.
 * 目前支持的Iot数据类型
 *
 * @author nomelin
 * @since 2024/12/27 15:43
 **/
public enum IotDataType {
    INT, LONG, FLOAT, DOUBLE;

    public static TSDataType convertToTsDataType(IotDataType iotDataType) {
        return switch (iotDataType) {
            case INT -> TSDataType.INT32;
            case LONG -> TSDataType.INT64;
            case FLOAT -> TSDataType.FLOAT;
            case DOUBLE -> TSDataType.DOUBLE;
        };
    }
}
