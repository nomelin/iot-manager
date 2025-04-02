package top.nomelin.iot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * PushPlusConfig
 *
 * @author nomelin
 * @since 2025/04/01
 **/
@Configuration
@ConfigurationProperties(prefix = "pushplus")
public
class PushPlusConfig {
    private int dailyGlobalLimit;
    private int userTokenBucketSize;
    private int userTokenRefreshInterval;
    private String token;

    // getters and setters
    public int getDailyGlobalLimit() {
        return dailyGlobalLimit;
    }

    public void setDailyGlobalLimit(int dailyGlobalLimit) {
        this.dailyGlobalLimit = dailyGlobalLimit;
    }

    public int getUserTokenBucketSize() {
        return userTokenBucketSize;
    }

    public void setUserTokenBucketSize(int userTokenBucketSize) {
        this.userTokenBucketSize = userTokenBucketSize;
    }

    public int getUserTokenRefreshInterval() {
        return userTokenRefreshInterval;
    }

    public void setUserTokenRefreshInterval(int userTokenRefreshInterval) {
        this.userTokenRefreshInterval = userTokenRefreshInterval;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
