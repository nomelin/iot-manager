package top.nomelin.iot.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import top.nomelin.iot.cache.CacheOperations;
import top.nomelin.iot.cache.CacheResult;
import top.nomelin.iot.common.annotation.CacheOp;
import top.nomelin.iot.common.enums.CacheOpType;

import static top.nomelin.iot.common.Constants.ALL_ARGS_KEY;
import static top.nomelin.iot.common.Constants.log;

/**
 * 适用于简单的缓存场景。如果需要更复杂的缓存逻辑，此aop无法满足。
 */
@Aspect
@Component
public class CacheAspect {

    private final CacheOperations<String, Object> cacheOperations;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${cache.enabled:true}")
    private boolean cacheEnabled;

    public CacheAspect(CacheOperations<String, Object> cacheOperations) {
        this.cacheOperations = cacheOperations;
    }

    @PostConstruct
    public void init() {
        log.info("CacheAspect 已创建, cacheEnabled: {}", cacheEnabled);
    }

    @Around("@annotation(cacheOp)")
    public Object around(ProceedingJoinPoint joinPoint, CacheOp cacheOp) throws Throwable {
        if (!cacheEnabled) {
            // 缓存禁用，直接执行方法
            return joinPoint.proceed();
        }
//        log.info("执行缓存操作，注解信息：{}", cacheOp);
        // 解析SpEL表达式生成缓存键
        String key = evaluateKeyExpression(joinPoint, cacheOp.key());
        String prefix = cacheOp.prefix();
        String fullKey = StringUtils.hasText(prefix) ? prefix + "::" + key : key;

        CacheOpType opType = cacheOp.type();
        Object result;

        switch (opType) {
            case GET -> {
                // 先尝试从缓存获取
                CacheResult<Object> cacheResult = cacheOperations.get(fullKey);
                if (cacheResult.isHit()) {
                    return cacheResult.getData();
                }
                // 缓存未命中，执行方法并缓存结果
                log.warn("缓存未命中，执行方法并缓存结果, key:{}", fullKey);
                result = joinPoint.proceed();
                cacheOperations.put(fullKey, result);
            }
            case PUT -> {
                // 执行方法后更新缓存
                result = joinPoint.proceed();
                cacheOperations.put(fullKey, result);
            }
            case DELETE -> {
                // 执行方法后删除缓存
                result = joinPoint.proceed();
                cacheOperations.remove(fullKey);
            }
            default -> throw new IllegalArgumentException("Unsupported cache operation type: " + opType);
        }
        return result;
    }

    // 解析SpEL表达式生成缓存键
    private String evaluateKeyExpression(ProceedingJoinPoint joinPoint, String expressionStr) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(signature.getMethod());
        Object[] args = joinPoint.getArgs();


        // 如果 key 是特殊值，则使用所有参数构造 JSON 作为缓存键
        if (ALL_ARGS_KEY.equals(expressionStr)) {
            try {
                return objectMapper.writeValueAsString(args);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize key to JSON", e);
            }
        }

        EvaluationContext context = new StandardEvaluationContext();
        // 绑定参数名和参数值
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }
        // 绑定arg0, arg1等别名
        for (int i = 0; i < args.length; i++) {
            context.setVariable("arg" + i, args[i]);
        }

        try {
            Expression expression = expressionParser.parseExpression(expressionStr);
            Object value = expression.getValue(context);
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize key to JSON", e);
        }
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
        log.info("CacheAspect cacheEnabled 设置为: {}", cacheEnabled);
    }
}