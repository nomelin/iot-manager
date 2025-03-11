package top.nomelin.iot.service;


import cn.hutool.db.PageResult;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.nomelin.iot.cache.CurrentUserCache;
import top.nomelin.iot.common.Constants;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.dao.MessageMapper;
import top.nomelin.iot.model.Message;
import top.nomelin.iot.model.enums.MessageStatus;
import top.nomelin.iot.model.enums.MessageType;

import java.time.Instant;
import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(MessageServiceImpl.class);

    private final MessageMapper messageDao;
    private final CurrentUserCache currentUserCache;

    public MessageServiceImpl(MessageMapper messageDao, CurrentUserCache currentUserCache) {
        this.messageDao = messageDao;
        this.currentUserCache = currentUserCache;
    }

    @Override
    @Transactional
    public void sendSystemMessage(Integer receiveId, String title, String content, MessageType type) {
        Message message = new Message();
        message.setSendId(Constants.SYSTEM_SENDER_ID);
        message.setReceiveId(receiveId);
        message.setTitle(title);
        message.setContent(content);
        message.setType(type);
        message.setStatus(MessageStatus.UNREAD);
        message.setCreateTime(Instant.now().getEpochSecond());

        messageDao.insert(message);
        log.info("系统消息发送成功 | receiveId:{} | title:{} | type:{}", receiveId, title, type);
    }

    @Override
    public Message getMessage(Integer messageId) {
        Message message = checkMessagePermission(messageId);
        log.debug("获取消息详情 | messageId:{}", messageId);
        return message;
    }

    @Override
    @Transactional
    public void markMessageStatus(Integer messageId, MessageStatus status) {
        checkMessagePermission(messageId);
        Long updateTime = Instant.now().getEpochSecond();
        messageDao.updateStatus(messageId, status, updateTime);
        log.info("更新消息状态 | messageId:{} | status:{}", messageId, status);
    }

    @Override
    public PageResult<Message> listMessages(MessageType type, MessageStatus status,
                                            Integer pageNum, Integer pageSize) {
        int currentUserId = currentUserCache.getCurrentUser().getId();
        int offset = (pageNum - 1) * pageSize;

        List<Message> messages = messageDao.selectByReceiveId(
                currentUserId, type, status, offset, pageSize
        );
        int total = messageDao.countByReceiveId(currentUserId, type, status);

        //TODO 分页查询未完成
        return new PageResult<Message>(pageNum, pageSize, total);
    }

    @Override
    public int getUnreadCount() {
        int currentUserId = currentUserCache.getCurrentUser().getId();
        return messageDao.countByReceiveId(
                currentUserId, null, MessageStatus.UNREAD
        );
    }

    // 权限校验统一方法
    private Message checkMessagePermission(Integer messageId) {
        Message message = messageDao.selectById(messageId);
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
