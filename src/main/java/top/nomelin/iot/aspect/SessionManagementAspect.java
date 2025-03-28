package top.nomelin.iot.aspect;

import org.apache.iotdb.isession.util.Version;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.session.Session;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
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

    @Value("${iotdb.ip}")
    private String ip;

    @Value("${iotdb.port}")
    private int port;

    @Value("${iotdb.username}")
    private String username;

    @Value("${iotdb.password}")
    private String password;

    @Before("execution(* top.nomelin.iot.dao.impl.IoTDBDaoImpl.*(..))")
    public void beforeMethod(JoinPoint joinPoint) {
        //如果当前线程没有Session，则创建Session
        if (SessionContext.getCurrentSession() == null) {
            createSession();
        }
        Session session = SessionContext.getCurrentSession();
        openSession(session);//打开Session
    }

    @After("execution(* top.nomelin.iot.dao.impl.IoTDBDaoImpl.*(..))")
    public void afterMethod(JoinPoint joinPoint) {
        Session session = SessionContext.getCurrentSession();
        closeSession(session);//关闭Session
    }

    private void createSession() {
        Session session = new Session.Builder()
                .host(ip)
                .port(port)
                .username(username)
                .password(password)
                .version(Version.V_1_0)
                .build();
        try {
            session.open(false);
        } catch (IoTDBConnectionException e) {
            log.error("创建Session失败: {}, error: {}", e.getMessage(), e);
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
        SessionContext.setCurrentSession(session);
    }

    private void openSession(Session session) {
        if (session != null) {
            try {
                session.open(false);
            } catch (IoTDBConnectionException e) {
                log.error("打开Session失败: {}, error: {}", e.getMessage(), e);
                throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
            }
        }
    }

    private void closeSession(Session session) {
        try {
            if (session != null) {
                session.close();
            }
        } catch (IoTDBConnectionException e) {
            log.error("关闭Session失败: {}, error: {}", e.getMessage(), e);
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
    }
}