package top.nomelin.iot.util;

import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.SystemException;
import top.nomelin.iot.model.enums.IotDataType;

/**
 * NumUtil
 *
 * @author nomelin
 * @since 2025/03/15 19:35
 **/
public class NumUtil {
    public static Object convertValue(Object value, IotDataType targetType) {
        if (value == null) {
            throw new SystemException(CodeMessage.DATA_CONVERSION_ERROR, "值为null，无法转换");
        }

        try {
            return switch (targetType) {
                case INT -> convertToInt(value);
                case LONG -> convertToLong(value);
                case FLOAT -> convertToFloat(value);
                case DOUBLE -> convertToDouble(value);
                case STRING -> convertToString(value);
                default ->
                        throw new SystemException(CodeMessage.DATA_CONVERSION_ERROR, "不支持的数据类型: " + targetType);
            };
        } catch (Exception e) {
            throw new SystemException(CodeMessage.DATA_CONVERSION_ERROR,
                    "无法将值 '" + value + "' (" + value.getClass().getSimpleName() + ") 转换为 " + targetType + ": " + e.getMessage(), e);
        }
    }

    public static Integer convertToInt(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            double doubleValue = ((Number) value).doubleValue();
            if (doubleValue % 1 != 0) {
                throw new NumberFormatException("值包含小数部分，无法转换为整数: " + doubleValue);
            }
            if (doubleValue < Integer.MIN_VALUE || doubleValue > Integer.MAX_VALUE) {
                throw new NumberFormatException("值超出Integer范围: " + doubleValue);
            }
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                throw new NumberFormatException("字符串无法解析为整数: " + value);
            }
        } else {
            throw new IllegalArgumentException("不支持的类型转换到INT: " + value.getClass().getName());
        }
    }

    public static Long convertToLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                throw new NumberFormatException("字符串无法解析为长整数: " + value);
            }
        } else {
            throw new IllegalArgumentException("不支持的类型转换到LONG: " + value.getClass().getName());
        }
    }

    public static Float convertToFloat(Object value) {
        if (value instanceof Float) {
            return (Float) value;
        } else if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else if (value instanceof String) {
            try {
                return Float.parseFloat((String) value);
            } catch (NumberFormatException e) {
                throw new NumberFormatException("字符串无法解析为浮点数: " + value);
            }
        } else {
            throw new IllegalArgumentException("不支持的类型转换到FLOAT: " + value.getClass().getName());
        }
    }

    public static Double convertToDouble(Object value) {
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                throw new NumberFormatException("字符串无法解析为双精度浮点数: " + value);
            }
        } else {
            throw new IllegalArgumentException("不支持的类型转换到DOUBLE: " + value.getClass().getName());
        }
    }

    public static String convertToString(Object value) {
        return value.toString();
    }
}
