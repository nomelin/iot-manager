package top.nomelin.iot.common.annotation;

import top.nomelin.iot.common.enums.CacheOpType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheOp {
    CacheOpType type();
    String key();
    String prefix() default "";
}
