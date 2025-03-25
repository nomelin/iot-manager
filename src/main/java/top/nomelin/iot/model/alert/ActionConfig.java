package top.nomelin.iot.model.alert;

import com.fasterxml.jackson.annotation.JsonIgnore;
import top.nomelin.iot.model.enums.MessageType;

import java.util.List;

/**
 * 告警动作配置实体
 *
 * @author nomelin
 * @since 2025/03/19 20:05
 **/
public class ActionConfig {
    private List<Integer> notifyUsers;    // 通知用户ID列表
    private List<AlertChannel> channels;       // 通知渠道
    private MessageType messageType;    // 消息类型(告警级别)
    private String extraMessage;    // 额外信息

    private Integer silentDuration; // 静默时间（单位：秒)。0表示不静默

    public ActionConfig() {
    }

    public ActionConfig(List<Integer> notifyUsers, List<AlertChannel> channels, MessageType messageType,
                        String extraMessage) {
        this.notifyUsers = notifyUsers;
        this.channels = channels;
        this.messageType = messageType;
        this.extraMessage = extraMessage;
        this.silentDuration = 0;
    }

    public ActionConfig(List<Integer> notifyUsers, List<AlertChannel> channels, MessageType messageType,
                        String extraMessage, Integer silentDuration) {
        this.notifyUsers = notifyUsers;
        this.channels = channels;
        this.messageType = messageType;
        this.extraMessage = extraMessage;
        this.silentDuration = silentDuration;
    }

    @JsonIgnore
    public boolean isValid() {
        return notifyUsers!= null && channels!= null && messageType!= null;
    }
    public List<Integer> getNotifyUsers() {
        return notifyUsers;
    }

    public void setNotifyUsers(List<Integer> notifyUsers) {
        this.notifyUsers = notifyUsers;
    }

    public List<AlertChannel> getChannels() {
        return channels;
    }

    public void setChannels(List<AlertChannel> channels) {
        this.channels = channels;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getExtraMessage() {
        return extraMessage;
    }

    public void setExtraMessage(String extraMessage) {
        this.extraMessage = extraMessage;
    }

    public Integer getSilentDuration() {
        return silentDuration;
    }

    public void setSilentDuration(Integer silentDuration) {
        this.silentDuration = silentDuration;
    }

    @Override
    public String toString() {
        return "ActionConfig{" +
                "notifyUsers=" + notifyUsers +
                ", channels=" + channels +
                ", messageType=" + messageType +
                ", extraMessage='" + extraMessage + '\'' +
                ", silentDuration=" + silentDuration +
                '}';
    }
}
