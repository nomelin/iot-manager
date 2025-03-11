package top.nomelin.iot.service;

import cn.hutool.db.PageResult;
import top.nomelin.iot.model.Message;
import top.nomelin.iot.model.enums.MessageStatus;
import top.nomelin.iot.model.enums.MessageType;

/**
 * MessageService
 *
 * @author nomelin
 * @since 2025/03/11 22:38
 **/
public interface MessageService {
    /**
     * 系统发送消息
     */
    void sendSystemMessage(Integer receiveId, String title, String content, MessageType type);

    /**
     * 获取消息详情
     */
    Message getMessage(Integer messageId);

    /**
     * 标记消息状态（已读/删除/标记）
     */
    void markMessageStatus(Integer messageId, MessageStatus status);

    /**
     * 分页获取消息列表
     */
    PageResult<Message> listMessages(MessageType type, MessageStatus status,
                                     Integer pageNum, Integer pageSize);

    /**
     * 获取未读消息数量
     */
    int getUnreadCount();
}
