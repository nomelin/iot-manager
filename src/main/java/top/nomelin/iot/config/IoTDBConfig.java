package top.nomelin.iot.config;

import org.apache.iotdb.isession.util.Version;
import org.apache.iotdb.rpc.IoTDBConnectionException;
import org.apache.iotdb.session.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IoTDBConfig {
    @Value("${iotdb.ip}")
    private String ip; // IoTDB的IP地址
    @Value("${iotdb.port}")
    private int port; // IoTDB的端口
    @Value("${iotdb.username}")
    private String username; // 用户名
    @Value("${iotdb.password}")
    private String password; // 密码

    @Bean
    public Session IoTDBSession() throws IoTDBConnectionException {
        Session session = new Session.Builder()
                .host(ip)
                .port(port)
                .username(username)
                .password(password)
                .version(Version.V_1_0)
                .build();
        session.open(false);
        return session;
    }
}
