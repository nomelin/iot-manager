package top.nomelin.iot.dao.session;

import org.apache.iotdb.session.Session;

/**
 * 保持每一个线程只使用一个Session实例。不同线程则从线程池中取用。
 */
public class SessionContext {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SessionContext.class);
    private static final ThreadLocal<Session> currentSession = new ThreadLocal<>();

    public static Session getCurrentSession() {
        Session session = currentSession.get();
        if (session == null) {
            throw new IllegalStateException("没有Session绑定到当前线程");
        }
        return session;
    }

    public static void bind(Session session) {
        if (currentSession.get() != null) {
            log.warn("当前线程已绑定Session");
        }
        currentSession.set(session);
    }

    public static void unbind() {
        currentSession.remove();
    }
}

