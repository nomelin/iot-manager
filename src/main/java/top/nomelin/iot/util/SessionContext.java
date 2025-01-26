package top.nomelin.iot.util;

import org.apache.iotdb.session.Session;

public class SessionContext {
    private static final ThreadLocal<Session> currentSession = new ThreadLocal<>();

    public static Session getCurrentSession() {
        return currentSession.get();
    }

    public static void setCurrentSession(Session session) {
        currentSession.set(session);
    }

    public static void clear() {
        currentSession.remove();
    }
}
