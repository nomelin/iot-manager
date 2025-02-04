package top.nomelin.iot.model.enums;

public enum FileTaskStatus {
    QUEUED("任务排队中"),
    PROCESSING("任务正在处理"),
    PAUSED("任务已暂停"),
    COMPLETED("任务已完成"),
    FAILED("任务失败"),
    CANCELLED("任务已取消");
    private final String name;

    FileTaskStatus(String name) {
        this.name = name;
    }
}
