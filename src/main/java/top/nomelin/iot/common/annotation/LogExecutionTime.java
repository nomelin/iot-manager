package top.nomelin.iot.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法执行耗时日志注解
 * 可用在类或方法上，优先使用方法级别的配置
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface LogExecutionTime {
    /**
     * 耗时警告阈值（毫秒），默认3000ms
     */
    long warnThreshold() default 3000L;

    /**
     * 是否记录入参
     */
    boolean logArgs() default false;

    /**
     * 是否记录返回值
     */
    boolean logReturn() default false;
}
