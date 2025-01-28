package top.nomelin.iot.service.aggregation;

import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.SystemException;

import java.util.List;
import java.util.Objects;

/**
 * Aggregator
 *
 * @author nomelin
 * @since 2025/01/28 15:36
 **/
public class Aggregator {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Aggregator.class);

    public static Object aggregate(List<Object> values, QueryMode mode) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        return switch (mode) {
            case AVG -> values.stream()
                    .filter(Objects::nonNull)
                    .mapToDouble(v -> ((Number) v).doubleValue())
                    .average().orElse(0);
            case MAX -> values.stream()
                    .filter(Objects::nonNull)
                    .mapToDouble(v -> ((Number) v).doubleValue())
                    .max().orElse(0);
            case MIN -> values.stream()
                    .filter(Objects::nonNull)
                    .mapToDouble(v -> ((Number) v).doubleValue())
                    .min().orElse(0);
            case SUM -> values.stream()
                    .filter(Objects::nonNull)
                    .mapToDouble(v -> ((Number) v).doubleValue())
                    .sum();
            case COUNT -> values.size();
            case FIRST -> values.get(0);
            case LAST -> values.get(values.size() - 1);
            default -> {
                log.error("Illegal aggregation mode: {}", mode);
                throw new SystemException(CodeMessage.ILLEGAL_AGGREGATION_ERROR);
            }
        };
    }
}
