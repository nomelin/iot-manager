package top.nomelin.iot.model.alert;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * 触发条件配置，简单版。
 *
 * @author nomelin
 * @since 2025/03/19 21:13
 **/
public class SimpleConditionConfig {
    private Integer duration;  // 持续时长（单位：秒）。若持续时长为0，则表示只要满足条件就触发
    private String metric;   // 监测指标（如：temperature）
    private Double maxValue;  // >= 最大阈值，若为null，则表示不设置最大阈值。必须设置一种阈值。
    private Double minValue;  // <= 最小阈值，若为null，则表示不设置最小阈值。必须设置一种阈值。

    /**
     * @return 此触发条件是否合法。
     */
    public boolean isValid() {
        return duration != null && duration >= 0
                && StringUtils.isNotEmpty(metric)
                && (maxValue != null || minValue != null);
    }

    /**
     * 评估是否满足触发条件
     *
     * @param metrics 数据点键值对
     * @return 是否满足触发条件
     */
    public boolean evaluate(Map<String, Object> metrics) {
        Object value = metrics.get(getMetric());
        if (value == null) {
            return false;
        }
        double numericValue;
        try {
            numericValue = Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return false;// 非数值类型，不进行比较
        }
        if (maxValue != null && numericValue >= maxValue) {
            return true;
        }
        if (minValue != null && numericValue <= minValue) {
            return true;
        }
        return false;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getMetric() {
        return metric;
    }

    public void setMetric(String metric) {
        this.metric = metric;
    }

    public Double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Double maxValue) {
        this.maxValue = maxValue;
    }

    public Double getMinValue() {
        return minValue;
    }

    public void setMinValue(Double minValue) {
        this.minValue = minValue;
    }
}
