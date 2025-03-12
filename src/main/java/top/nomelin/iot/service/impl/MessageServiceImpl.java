package top.nomelin.iot.service.impl;


import cn.hutool.core.util.ObjectUtil;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import top.nomelin.iot.cache.CurrentUserCache;
import top.nomelin.iot.common.Constants;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.dao.MessageMapper;
import top.nomelin.iot.model.Message;
import top.nomelin.iot.model.enums.MessageStatus;
import top.nomelin.iot.model.enums.MessageType;
import top.nomelin.iot.service.MessageService;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class MessageServiceImpl implements MessageService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MessageServiceImpl.class);

    private final MessageMapper messageMapper;
    private final CurrentUserCache currentUserCache;

    public MessageServiceImpl(MessageMapper messageMapper, CurrentUserCache currentUserCache) {
        this.messageMapper = messageMapper;
        this.currentUserCache = currentUserCache;
    }

    @Override
    public void sendSystemMessage(Integer receiveId, String title, String content, MessageType type) {
        Message message = new Message();
        message.setSendId(Constants.SYSTEM_SENDER_ID);
        message.setReceiveId(receiveId);
        message.setTitle(title);
        message.setContent(content);
        message.setType(type);
        message.setStatus(MessageStatus.UNREAD);
        message.setCreateTime(Instant.now().getEpochSecond());

        messageMapper.insert(message);
        log.info("系统消息发送成功 | receiveId:{} | title:{} | type:{}", receiveId, title, type);
    }

    @Override
    public Message getMessage(Integer messageId) {
        Message message = checkMessagePermission(messageId);
        log.info("获取消息详情 | messageId:{}", messageId);
        return message;
    }

    @Override
    public void markMessageStatus(Integer messageId, MessageStatus status) {
        Message message = checkMessagePermission(messageId);
        //获取毫秒级时间戳
        Long updateTime = System.currentTimeMillis();
        if (message.getStatus() == status) {
            log.warn("消息状态未改变 | messageId:{} | status:{}", messageId, status);
            return;
        }
        if (status == MessageStatus.UNREAD) {
            log.warn("消息状态不能设置为未读 | messageId:{} | status:{}", messageId, status);
            throw new BusinessException(CodeMessage.INVALID_STATUS_ERROR);
        }
        MessageStatus oldStatus = message.getStatus();
        if (oldStatus == MessageStatus.UNREAD && status == MessageStatus.MARKED) {
            log.warn("消息状态不能从未读直接变为标记 | messageId:{} | status:{}", messageId, status);
            throw new BusinessException(CodeMessage.INVALID_STATUS_ERROR);
        }
        // 只有未读消息变为已读时才更新已读时间。比如标记状态取消变为已读，则不更新已读时间
        if (status == MessageStatus.READ && oldStatus == MessageStatus.UNREAD) {
            message.setReadTime(updateTime);
        } else if (status == MessageStatus.DELETED) {
            message.setDeleteTime(updateTime);
        }
        message.setStatus(status);
        messageMapper.updateById(message);
        log.info("更新消息状态 | messageId:{} | status:{}->{}", messageId, oldStatus, status);
    }

    @Override
    public void deleteMessageBatch(List<Integer> messageIds) {
        int currentUserId = currentUserCache.getCurrentUser().getId();
        for (Integer messageId : messageIds) {
            markMessageStatus(messageId, MessageStatus.DELETED);
        }
        log.info("批量删除消息 | messageIds:{}", messageIds);
    }


    @Override
    public List<Message> getAllSimpleMessages(MessageType type, MessageStatus status) {
        int currentUserId = currentUserCache.getCurrentUser().getId();
        Message message = new Message();
        message.setReceiveId(currentUserId);
        message.setType(type);
        message.setStatus(status);
        log.info("获取用户消息列表 | type:{} | status:{}", type, status);
        return messageMapper.selectSimpleAll(message);
    }

    @Override
    public List<Message> getAllSimpleMessages(MessageType type, MessageStatus status, String keyword) {
        if (ObjectUtil.isEmpty(keyword)) {
            return getAllSimpleMessages(type, status);
        }
        int currentUserId = currentUserCache.getCurrentUser().getId();
        Message message = new Message();
        message.setReceiveId(currentUserId);
        message.setType(type);
        message.setStatus(status);
        //搜索标题
        message.setTitle(keyword);
        List<Message> messages = messageMapper.selectSimpleAll(message);
        //搜索内容
        message.setContent(keyword);
        message.setTitle(null);
        List<Message> messages2 = messageMapper.selectSimpleAll(message);
        //去重
        Set<Message> set = new java.util.HashSet<>();
        set.addAll(messages);
        set.addAll(messages2);
        messages = new java.util.ArrayList<>(set);
        log.info("获取用户消息列表 | type:{} | status:{} | keyword:{}", type, status, keyword);
        return messages;
    }

    @Override
    public int getUnreadCount() {
        int currentUserId = currentUserCache.getCurrentUser().getId();
        return messageMapper.countByReceiveId(
                currentUserId, null, MessageStatus.UNREAD
        );
    }

    // 权限校验统一方法
    private Message checkMessagePermission(Integer messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            log.warn("消息不存在 | messageId:{}", messageId);
            throw new BusinessException(CodeMessage.NOT_FOUND_ERROR);
        }

        int currentUserId = currentUserCache.getCurrentUser().getId();
        if (!message.getReceiveId().equals(currentUserId)) {
            log.warn("无权访问消息 | messageId:{} | currentUserId:{}", messageId, currentUserId);
            throw new BusinessException(CodeMessage.NO_PERMISSION_ERROR);
        }
        return message;
    }
}
