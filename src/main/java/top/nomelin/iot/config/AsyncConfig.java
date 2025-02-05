package top.nomelin.iot.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.common.exception.SystemException;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步配置类
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AsyncConfig.class);

    @Value("${system.env}")
    private String env;

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("FileProcessor-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    //Spring 异步方法中的异常不会直接被全局异常处理器捕获，因为这些异常是在线程池中的子线程中抛出的，而不是主线程中。
    //TODO 异常处理整合
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            // 这里可以处理异步线程中抛出的异常
            if (throwable instanceof SystemException e) {
                log.error("[异步]系统异常->{}", e, e);
                e.printStackTrace();
                if (e.cause != null) {
                    e.cause.printStackTrace();
                }
            } else if (throwable instanceof BusinessException e) {
                if ("dev".equals(env)) {// 开发环境才显示业务异常信息
                    log.warn("[异步]业务异常->{}", e, e);
                    e.printStackTrace();
                    if (e.cause != null) {
                        e.cause.printStackTrace();
                    }
                }
            } else {
                log.error("其它异常->{}", throwable, throwable);
                throwable.printStackTrace();
            }
        };
    }
}
