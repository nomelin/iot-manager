package top.nomelin.iot.model;

import top.nomelin.iot.model.enums.MessageStatus;
import top.nomelin.iot.model.enums.MessageType;

import java.util.Objects;

/**
 * Message
 *
 * @author nomelin
 * @since 2025/03/11 21:30
 **/

public class Message {
    private Integer id;
    private Integer sendId;
    private Integer receiveId;
    private String title;
    private String content;
    private MessageType type;
    private MessageStatus status;
    private Long createTime;
    private Long readTime;
    private Long deleteTime;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSendId() {
        return sendId;
    }

    public void setSendId(Integer sendId) {
        this.sendId = sendId;
    }

    public Integer getReceiveId() {
        return receiveId;
    }

    public void setReceiveId(Integer receiveId) {
        this.receiveId = receiveId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getReadTime() {
        return readTime;
    }

    public void setReadTime(Long readTime) {
        this.readTime = readTime;
    }

    public Long getDeleteTime() {
        return deleteTime;
    }

    public void setDeleteTime(Long deleteTime) {
        this.deleteTime = deleteTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Message message = (Message) o;
        return id.equals(message.id) && sendId.equals(message.sendId) && receiveId.equals(message.receiveId) && Objects.equals(title, message.title) && Objects.equals(content, message.content) && type == message.type && status == message.status && createTime.equals(message.createTime) && Objects.equals(readTime, message.readTime) && Objects.equals(deleteTime, message.deleteTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sendId, receiveId, title, content, type, status, createTime, readTime, deleteTime);
    }
}
