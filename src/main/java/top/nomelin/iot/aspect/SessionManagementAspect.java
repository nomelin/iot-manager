package top.nomelin.iot.aspect;

import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.rpc.StatementExecutionException;
import org.apache.iotdb.session.Session;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.SystemException;
import top.nomelin.iot.util.SessionContext;

/**
 * 每个线程都会有一个Session，因此需要在每个DAO方法调用前创建Session，并在方法调用后关闭Session。
 * 统一异常处理，并在方法调用后清理Session。
 */
@Aspect
@Component
public class SessionManagementAspect {
    private static final Logger log = LoggerFactory.getLogger(SessionManagementAspect.class);

    @Value("${iotdb.host}")
    private String host;

    @Value("${iotdb.port}")
    private String port;

    @Value("${iotdb.username}")
    private String username;

    @Value("${iotdb.password}")
    private String password;

    @Around("execution(* top.nomelin.iot.dao.impl.IoTDBDaoImpl.*(..))")
    public Object manageSession(ProceedingJoinPoint joinPoint) {
        Session session = new Session.Builder()
                .host(host)
                .port(Integer.parseInt(port))
                .username(username)
                .password(password)
                .build();
        try {
            session.open();
            SessionContext.setCurrentSession(session);
            return joinPoint.proceed();
        } catch (Throwable e) {
            handleException(e); // 统一异常处理
            return null;
        } finally {
            cleanUpSession(session);
        }
    }

    private void handleException(Throwable e) {
        if (e instanceof IoTDBConnectionException || e instanceof StatementExecutionException) {
            log.error("IoTDB操作失败: {}", e.getMessage());
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        } else {
            log.error("未知错误: {}", e.getMessage(), e);
            throw new SystemException(CodeMessage.UNKNOWN_ERROR, e);
        }
    }

    private void cleanUpSession(Session session) {
        SessionContext.clear();
        try {
            if (session != null) {
                session.close();
            }
        } catch (IoTDBConnectionException e) {
            log.error("关闭Session失败: {}", e.getMessage());
        }
    }
}