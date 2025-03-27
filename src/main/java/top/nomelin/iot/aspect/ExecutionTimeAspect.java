package top.nomelin.iot.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import top.nomelin.iot.common.annotation.LogExecutionTime;

@Aspect
@Component
public class ExecutionTimeAspect {
    private static final Logger log = LoggerFactory.getLogger(ExecutionTimeAspect.class);

    @Around("@within(logExecutionTime) || @annotation(logExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint, LogExecutionTime logExecutionTime) throws Throwable {
        // 获取实际注解配置（优先方法级别）
        LogExecutionTime effectiveAnnotation = AnnotationUtils.findAnnotation(
                ((MethodSignature) joinPoint.getSignature()).getMethod(),
                LogExecutionTime.class
        );
//        if (effectiveAnnotation == null) {
//            effectiveAnnotation = logExecutionTime;
//        }

        final StopWatch stopWatch = new StopWatch();
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final String className = signature.getDeclaringType().getSimpleName();
        final String methodName = signature.getName();

        try {
            logMethodStart(effectiveAnnotation, className, methodName, joinPoint.getArgs());

            stopWatch.start();
            Object result = joinPoint.proceed();
            stopWatch.stop();

            logMethodEnd(effectiveAnnotation, className, methodName, stopWatch.getTotalTimeMillis(), result);

            return result;
        } catch (Throwable t) {
            stopWatch.stop();
            log.warn("[{}#{}] 执行异常 | 耗时: {}ms | 异常类型: {}, 异常信息: {}",
                    className, methodName,
                    stopWatch.getTotalTimeMillis(),
                    t.getClass().getSimpleName(),
                    t.toString());
            throw t;
        }
    }

    private void logMethodStart(LogExecutionTime annotation,
                                String className,
                                String methodName,
                                Object[] args) {
        if (annotation.logArgs()) {
            log.debug("[{}#{}] 方法执行开始 | 参数: {}",
                    className, methodName, formatArguments(args));
        }
    }

    private void logMethodEnd(LogExecutionTime annotation,
                              String className,
                              String methodName,
                              long executionTime,
                              Object result) {
        String messageTemplate = "[{}#{}] 方法执行结束 | 耗时: {}ms";
        Object[] params = new Object[]{className, methodName, executionTime};

        if (annotation.logReturn()) {
            messageTemplate += " | 返回结果: {}";
            params = new Object[]{className, methodName, executionTime, formatReturnValue(result)};
        }

        if (executionTime > annotation.warnThreshold()) {
            log.warn(messageTemplate, params);
        } else {
            log.info(messageTemplate, params);
        }
    }

    private String formatArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "无参数";
        }
        // 简单实现，实际生产环境可能需要更安全的处理
        return java.util.Arrays.toString(args);
    }

    private String formatReturnValue(Object result) {
        if (result == null) {
            return "void";
        }
        // 避免输出大对象
        return result.toString().length() > 100 ?
                result.getClass().getSimpleName() + "@" + Integer.toHexString(result.hashCode())
                : result.toString();
    }
}