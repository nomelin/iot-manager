package top.nomelin.iot.dao.session;

import jakarta.annotation.PostConstruct;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.iotdb.isession.util.Version;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.session.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.SystemException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * SessionPool
 *
 * @author nomelin
 * @since 2025/04/21
 **/
@Component
public class SessionPool {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SessionPool.class);
    private final GenericObjectPool<Session> pool;

    @Value("${iotdb.session.pool.max:30}")
    private int maxTotal;

    @Value("${iotdb.session.pool.min-idle:10}")
    private int minIdle;

    @Value("${iotdb.session.pool.max-wait-seconds:30}")
    private int maxWaitSeconds;

    public SessionPool(
            @Value("${iotdb.ip}") String ip,
            @Value("${iotdb.port}") int port,
            @Value("${iotdb.username}") String username,
            @Value("${iotdb.password}") String password) {

        this.pool = new GenericObjectPool<>(new SessionFactory(ip, port, username, password));
    }

    //必须在构造方法之后调用，否则参数还未注入
    @PostConstruct
    private void configurePool() {
        GenericObjectPoolConfig<Session> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(maxTotal);                // 最大连接数
        config.setMinIdle(minIdle);                  // 最小空闲连接数
        config.setMaxWait(Duration.ofSeconds(maxWaitSeconds)); // 最大等待时间（获取连接时阻塞的时间）
        config.setTestOnBorrow(true);          // 获取连接时验证
        config.setTestWhileIdle(true);         // 空闲时验证
        pool.setConfig(config);
        log.info("SessionPool配置: maxTotal={}, minIdle={}, maxWaitSeconds={}", maxTotal, minIdle, maxWaitSeconds);
    }

    /**
     * 取出Session
     */
    public Session borrowSession() {
        try {
            return pool.borrowObject();
        } catch (Exception e) {
            throw new SystemException(CodeMessage.IOT_DB_ERROR, e);
        }
    }

    /**
     * 归还Session
     */
    public void returnSession(Session session) {
        if (session != null) {
            pool.returnObject(session);
        }
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("maxTotal", pool.getMaxTotal());
        stats.put("numActive", pool.getNumActive());
        stats.put("numIdle", pool.getNumIdle());
        stats.put("minIdle", pool.getMinIdle());
        stats.put("numWaiters", pool.getNumWaiters());
        stats.put("createdCount", pool.getCreatedCount());
        stats.put("borrowedCount", pool.getBorrowedCount());
        stats.put("returnedCount", pool.getReturnedCount());
        stats.put("destroyedCount", pool.getDestroyedCount());
        return stats;
    }


    private static class SessionFactory extends BasePooledObjectFactory<Session> {
        private final String ip;
        private final int port;
        private final String username;
        private final String password;

        SessionFactory(String ip, int port, String username, String password) {
            this.ip = ip;
            this.port = port;
            this.username = username;
            this.password = password;
        }

        @Override
        public Session create() throws Exception {
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
            return session;
        }

        @Override
        public PooledObject<Session> wrap(Session session) {
            return new DefaultPooledObject<>(session);
        }

        @Override
        public void destroyObject(PooledObject<Session> p) {
            try {
                p.getObject().close();
            } catch (IoTDBConnectionException e) {
                log.error("销毁Session失败: {}, error: {}", e.getMessage(), e);
            }
        }

        @Override
        public boolean validateObject(PooledObject<Session> p) {
            try {
                p.getObject().executeNonQueryStatement("show databases");
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
