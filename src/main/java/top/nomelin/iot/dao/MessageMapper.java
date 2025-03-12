package top.nomelin.iot.dao;

import org.apache.ibatis.annotations.Mapper;
import top.nomelin.iot.model.Message;
import top.nomelin.iot.model.enums.MessageStatus;
import top.nomelin.iot.model.enums.MessageType;

import java.util.List;

/**
 * MessageMapper
 *
 * @author nomelin
 * @since 2025/03/11 21:59
 **/
@Mapper
public interface MessageMapper {
    int insert(Message message);

    int updateById(Message message);

    /**
     * 硬删除消息，最好不要使用。软删除可以用updateById方法，将status字段设置为[删除]
     */
    int delete(Integer id);

    Message selectById(Integer id);

    /**
     * 查询消息列表,返回的是完整的消息对象，包含[删除]的消息
     */
    List<Message> selectAll(Message message);

    /**
     * 查询消息列表，不包含消息内容字段，不包含[删除]的消息
     */
    List<Message> selectSimpleAll(Message message);

    int countByReceiveId(Integer receiveId, MessageType type, MessageStatus status);
}
