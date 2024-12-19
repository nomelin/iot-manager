package top.nomelin.iot.cache;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import top.nomelin.iot.model.User;

/**
 * 缓存会话的当前用户信息
 *
 * @author nomelin
 */
@Component
@SessionScope
public class CurrentUserCache {
    private User currentUser = null;

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public void clear() {
        this.currentUser = null;
    }
}
