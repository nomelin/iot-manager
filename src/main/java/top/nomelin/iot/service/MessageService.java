package top.nomelin.iot.service;

import top.nomelin.iot.model.Message;
import top.nomelin.iot.model.enums.MessageStatus;
import top.nomelin.iot.model.enums.MessageType;

import java.util.List;

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
     * 删除消息
     */
    void deleteMessageBatch(List<Integer> messageIds);

    /**
     * 获取简略消息列表，不包含消息content，不包含删除的消息
     */
    List<Message> getAllSimpleMessages(MessageType type, MessageStatus status);

    /**
     * 获取简略消息列表，不包含消息content，不包含删除的消息，根据关键字搜索
     */
    List<Message> getAllSimpleMessages(MessageType type, MessageStatus status, String keyword);

    /**
     * 获取未读消息数量
     */
    int getUnreadCount();
}
