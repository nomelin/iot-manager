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

    int update(Message message);

    int delete(Integer id);

    Message selectById(Integer id);

    List<Message> selectByReceiveId(Integer receiveId, MessageType type, MessageStatus status,
                                    Integer offset, Integer limit);

    int updateStatus(Integer id, MessageStatus status, Long updateTime);

    int countByReceiveId(Integer receiveId, MessageType type, MessageStatus status);
}
