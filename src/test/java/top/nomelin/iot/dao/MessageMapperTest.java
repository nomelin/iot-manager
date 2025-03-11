package top.nomelin.iot.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import top.nomelin.iot.model.Message;
import top.nomelin.iot.model.enums.MessageStatus;
import top.nomelin.iot.model.enums.MessageType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Transactional
@ActiveProfiles("test")
class MessageMapperTest {

    private final long currentTime = System.currentTimeMillis();
    @Autowired
    private MessageMapper messageMapper;
    private Message testMessage;


    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testMessage = new Message();
        testMessage.setSendId(0);
        testMessage.setReceiveId(1);
        testMessage.setTitle("系统通知");
        testMessage.setContent("这是一条测试消息");
        testMessage.setType(MessageType.NOTICE);
        testMessage.setStatus(MessageStatus.UNREAD);
        testMessage.setCreateTime(currentTime);
    }

    @Test
    void insertAndSelectById() {
        // 插入测试
        int result = messageMapper.insert(testMessage);
        assertEquals(1, result);
        assertNotNull(testMessage.getId());

        // 查询测试
        Message dbMessage = messageMapper.selectById(testMessage.getId());
        assertNotNull(dbMessage);
        assertEquals(testMessage.getTitle(), dbMessage.getTitle());
        assertEquals(MessageType.NOTICE, dbMessage.getType());
        assertEquals(MessageStatus.UNREAD, dbMessage.getStatus());
    }

    @Test
    void update() {
        messageMapper.insert(testMessage);

        // 修改字段
        testMessage.setTitle("更新后的标题");
        testMessage.setContent("更新后的内容");
        testMessage.setType(MessageType.WARNING);
        testMessage.setStatus(MessageStatus.READ);
        testMessage.setReadTime(currentTime + 1000);

        // 执行更新
        int result = messageMapper.update(testMessage);
        assertEquals(1, result);

        // 验证更新
        Message updated = messageMapper.selectById(testMessage.getId());
        assertEquals("更新后的标题", updated.getTitle());
        assertEquals(MessageType.WARNING, updated.getType());
        assertEquals(MessageStatus.READ, updated.getStatus());
    }

    @Test
    void updateStatus() {
        messageMapper.insert(testMessage);
        long updateTime = currentTime + 2000;

        // 测试标记为已读
        int result = messageMapper.updateStatus(testMessage.getId(), MessageStatus.READ, updateTime);
        assertEquals(1, result);
        Message readMsg = messageMapper.selectById(testMessage.getId());
        assertEquals(MessageStatus.READ, readMsg.getStatus());
        assertEquals(updateTime, readMsg.getReadTime());

        // 测试标记为删除
        result = messageMapper.updateStatus(testMessage.getId(), MessageStatus.DELETED, updateTime + 1000);
        assertEquals(1, result);
        Message deletedMsg = messageMapper.selectById(testMessage.getId());
        assertEquals(MessageStatus.DELETED, deletedMsg.getStatus());
        assertEquals(updateTime + 1000, deletedMsg.getDeleteTime());
    }

    @Test
    void selectByReceiveId() {
        // 插入三条测试数据
        messageMapper.insert(testMessage);
        insertTestMessage(1, MessageType.WARNING, MessageStatus.UNREAD);
        insertTestMessage(1, MessageType.ERROR, MessageStatus.READ);
        insertTestMessage(2, MessageType.NOTICE, MessageStatus.UNREAD); // 其他接收者

        // 测试基础查询
        List<Message> messages = messageMapper.selectByReceiveId(1, null, null, 0, 10);
        assertEquals(3, messages.size());

        // 测试类型过滤
        messages = messageMapper.selectByReceiveId(1, MessageType.NOTICE, null, 0, 10);
        assertEquals(1, messages.size());

        // 测试状态过滤
        messages = messageMapper.selectByReceiveId(1, null, MessageStatus.READ, 0, 10);
        assertEquals(1, messages.size());

        // 测试分页
        messages = messageMapper.selectByReceiveId(1, null, null, 0, 2);
        assertEquals(2, messages.size());
    }

    @Test
    void delete() {
        messageMapper.insert(testMessage);

        int result = messageMapper.delete(testMessage.getId());
        assertEquals(1, result);

        Message deleted = messageMapper.selectById(testMessage.getId());
        assertNull(deleted);
    }

    private void insertTestMessage(int receiveId, MessageType type, MessageStatus status) {
        Message msg = new Message();
        msg.setSendId(0);
        msg.setReceiveId(receiveId);
        msg.setTitle("Test");
        msg.setContent("Test Content");
        msg.setType(type);
        msg.setStatus(status);
        msg.setCreateTime(System.currentTimeMillis());
        messageMapper.insert(msg);
    }
}