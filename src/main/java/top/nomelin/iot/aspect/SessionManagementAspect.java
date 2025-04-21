package top.nomelin.iot.aspect;

import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.session.Session;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.SystemException;
import top.nomelin.iot.dao.session.SessionContext;
import top.nomelin.iot.dao.session.SessionPool;

/**
 * 新版Session管理切面，集成连接池与重试机制
 */
@Aspect
@Component
@Order(1) // 确保在事务切面之前执行
public class SessionManagementAspect {
    private static final Logger log = LoggerFactory.getLogger(SessionManagementAspect.class);
    private final SessionPool sessionPool;
    @Value("${iotdb.retry.max}")
    private int maxRetries;//不重试则为0，只会执行一次
    @Value("${iotdb.retry.initial-interval}")
    private int initialInterval;
    @Value("${iotdb.retry.multiplier}")
    private int multiplier;

    @Autowired
    public SessionManagementAspect(SessionPool sessionPool) {
        this.sessionPool = sessionPool;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        int oldMaxRetries = this.maxRetries;
        this.maxRetries = maxRetries;
        log.info("IoTDB 最大重试次数设置从 {} 调整为 {}", oldMaxRetries, maxRetries);
    }

    @Around("execution(* top.nomelin.iot.dao.impl.IoTDBDaoImpl.*(..))")
    public Object manageSession(ProceedingJoinPoint joinPoint) throws Throwable {
        Session session = null;
        int retryCount = 0;
        Throwable lastException = null;

        //错误重试机制
        while (retryCount <= maxRetries) {
            try {
                // 从连接池获取Session
                session = sessionPool.borrowSession();
                SessionContext.bind(session);

                log.debug("Session acquired [{}], retry {}/{}", session, retryCount, maxRetries);
                return joinPoint.proceed();

            } catch (IoTDBConnectionException | SystemException e) {
                // 记录异常并标记需要重试
                lastException = e;
                retryCount++;
                if (retryCount > maxRetries) {
                    log.warn("IoTDB操作失败，重试次数达到上限 ({}次), last exception: {}", maxRetries, e);
                    throw new SystemException(CodeMessage.IOT_DB_ERROR, "数据库重试全部失败", lastException);
                }
                log.warn("IoTDB操作失败，准备重试 (第{}/{}次)", retryCount, maxRetries, e);

                // 立即归还异常连接
                if (session != null) {
                    sessionPool.returnSession(session);
                    session = null;
                }

                // 等待间隔（指数退避）
                Thread.sleep(calculateBackoffInterval(retryCount));

            } finally {
                // 清理线程绑定
                SessionContext.unbind();

                // 正常归还连接
                if (session != null) {
                    sessionPool.returnSession(session);
                }
            }
        }

        throw new SystemException(CodeMessage.IOT_DB_ERROR, "数据库重试全部失败", lastException);//链式抛出最后一次的异常
    }

    private long calculateBackoffInterval(int retryCount) {
        return (long) (Math.pow(multiplier, retryCount - 1) * initialInterval); // 指数退避：100ms, 200ms, 400ms...
    }
}