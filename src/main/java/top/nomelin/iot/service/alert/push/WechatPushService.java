package top.nomelin.iot.service.alert.push;

import java.util.List;
import java.util.Map;

/**
 * PushPlus 微信推送服务
 *
 * @author nomelin
 * @since 2025/04/01
 **/
public interface WechatPushService {

    /**
     * 发送微信消息
     *
     * @param title        消息标题
     * @param content      消息内容
     * @param fromUserId   发送者用户 ID
     * @param friendTokens 好友令牌列表，以发送好友消息。
     * @return 是否发送成功
     */
    boolean send(String title, String content, int fromUserId, List<String> friendTokens);

    // 管理接口
    int getGlobalRemaining();

    void addGlobalRemaining(int delta);

    Map<String, Integer> getUserStatus(int userId);

    void clearUserQueue(int userId);
}
