package top.nomelin.iot.model.alert;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 告警触发条件配置
 *
 * @author nomelin
 * @since 2025/03/19 20:02
 **/
public class ConditionConfig {
    private static final Set<String> VALID_OPERATORS = Set.of(">", "<", ">=", "<=", "==", "!=");

    private ConditionNode root;  // 根节点，表示整个条件组合
    private Integer duration;  // 持续时长（单位：秒）。若持续时长为0，则表示只要满足条件就触发

    /**
     * 评估是否满足触发条件
     *
     * @param metrics 数据点键值对
     * @return 是否满足触发条件
     */
    public boolean evaluate(Map<String, Object> metrics) {
        if (root == null) {
            return false;
        }
        return evaluateNode(root, metrics);
    }

    private boolean evaluateNode(ConditionNode node, Map<String, Object> metrics) {
        if (node instanceof Condition) {
            return evaluateCondition((Condition) node, metrics);
        } else if (node instanceof ConditionGroup) {
            return evaluateConditionGroup((ConditionGroup) node, metrics);
        }
        throw new IllegalArgumentException("Unknown condition node type");
    }

    private boolean evaluateCondition(Condition condition, Map<String, Object> metrics) {
        Object value = metrics.get(condition.getMetric());
        if (value == null) {
            return false;// 物理量不存在，不进行比较
        }

        double numericValue;
        try {
            numericValue = Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return false;// 非数值类型，不进行比较
        }

        return switch (condition.getOperator()) {
            case ">" -> numericValue > condition.getThreshold();
            case "<" -> numericValue < condition.getThreshold();
            case ">=" -> numericValue >= condition.getThreshold();
            case "<=" -> numericValue <= condition.getThreshold();
            case "==" -> numericValue == condition.getThreshold();
            case "!=" -> numericValue != condition.getThreshold();
            default -> throw new IllegalArgumentException("Unsupported operator: " + condition.getOperator());
        };
    }

    private boolean evaluateConditionGroup(ConditionGroup group, Map<String, Object> metrics) {
        AlertConditionConnect connect = group.getConnect();
        List<ConditionNode> children = group.getChildren();

        if (connect == AlertConditionConnect.AND) {
            for (ConditionNode child : children) {
                if (!evaluateNode(child, metrics)) {
                    return false;
                }
            }
            return true;
        } else {
            for (ConditionNode child : children) {
                if (evaluateNode(child, metrics)) {
                    return true;
                }
            }
            return false;
        }
    }

    public ConditionNode getRoot() {
        return root;
    }

    public void setRoot(ConditionNode root) {
        this.root = root;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    /**
     * 条件节点（可以是条件组或单个条件）
     */
    public static abstract class ConditionNode {
    }

    /**
     * 叶子节点，表示单个条件
     */
    public static class Condition extends ConditionNode {
        private String metric;   // 监测指标（如：temperature）
        private String operator;   // 比较运算符（">="、"<="、"=="、">"、"<"）
        private Double threshold;  // 阈值。

        public Condition(String metric, String operator, Double threshold, Integer duration) {
            if (!VALID_OPERATORS.contains(operator)) {
                throw new IllegalArgumentException("Invalid operator: " + operator);
            }
            this.metric = metric;
            this.operator = operator;
            this.threshold = threshold;
        }

        public String getMetric() {
            return metric;
        }

        public void setMetric(String metric) {
            this.metric = metric;
        }

        public String getOperator() {
            return operator;
        }

        public void setOperator(String operator) {
            this.operator = operator;
        }

        public Double getThreshold() {
            return threshold;
        }

        public void setThreshold(Double threshold) {
            this.threshold = threshold;
        }
    }

    /**
     * 非叶子节点，表示一组条件的组合
     */
    public static class ConditionGroup extends ConditionNode {
        private List<ConditionNode> children;  // 子条件（可以是Condition或ConditionGroup）
        private AlertConditionConnect connect; // 连接类型（AND 或 OR）

        public ConditionGroup(List<ConditionNode> children, AlertConditionConnect connect) {
            if (children == null || children.isEmpty()) {
                throw new IllegalArgumentException("ConditionGroup must have at least one child");
            }
            this.children = children;
            this.connect = connect;
        }

        public List<ConditionNode> getChildren() {
            return children;
        }

        public void setChildren(List<ConditionNode> children) {
            this.children = children;
        }

        public AlertConditionConnect getConnect() {
            return connect;
        }

        public void setConnect(AlertConditionConnect connect) {
            this.connect = connect;
        }
    }
}
