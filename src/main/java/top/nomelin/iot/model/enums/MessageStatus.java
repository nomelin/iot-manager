package top.nomelin.iot.model.enums;

/**
 * MessageStatus
 *
 * @author nomelin
 * @since 2025/03/11 21:55
 **/

public enum MessageStatus {
    UNREAD(0, "未读"),
    READ(1, "已读"),
    DELETED(2, "删除"),
    MARKED(3, "标记");

    private final int code;
    private final String description;

    MessageStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
