package top.nomelin.iot.model.enums;

/**
 * MessageType
 *
 * @author nomelin
 * @since 2025/03/11 21:54
 **/

public enum MessageType {
    NOTICE(0, "通知"),
    WARNING(1, "警告"),
    ERROR(2, "错误");

    private final int code;
    private final String description;

    MessageType(int code, String description) {
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
