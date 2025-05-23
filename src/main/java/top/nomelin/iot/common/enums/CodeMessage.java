package top.nomelin.iot.common.enums;

public enum CodeMessage {
    SUCCESS("200", "成功"),

    PARAM_ERROR("400", "参数异常"),
    TOKEN_INVALID_ERROR("401", "无效的token"),
    TOKEN_CHECK_ERROR("402", "token验证失败，请重新登录"),
    TOKEN_PARSING_ERROR("403", "token解析失败，请重新登录"),
    NOT_FOUND_ERROR("404", "资源不存在"),
    PARAM_LOST_ERROR("405", "参数缺失"),
    ACCOUNT_CACHE_ERROR("406", "缓存读取失败,请重新登录"),
    TOKEN_EXPIRED_ERROR("407", "token已过期，请重新登录"),
    DEVICE_NOT_EXIST_ERROR("410", "设备不存在"),
    DB_DATA_ROW_ERROR("411", "数据库数据行异常"),


    USER_NAME_EXIST_ERROR("501", "用户名已存在"),
    USER_NOT_LOGIN_ERROR("502", "用户未登录"),
    USER_ACCOUNT_ERROR("503", "账号或密码错误"),
    USER_NOT_EXIST_ERROR("504", "用户不存在"),
    PARAM_PASSWORD_ERROR("505", "原密码输入错误"),
    USER_NAME_NULL_ERROR("506", "用户名缺失"),
    INVALID_USER_NAME_ERROR("507", "要操作的用户名不合法，无权限操作"),
    EQUAL_PASSWORD_ERROR("508", "新密码不能与原密码相同"),
    TASK_NOT_EXIST_ERROR("509", "任务不存在"),

    NO_PERMISSION_ERROR("601", "无权限"),

    DUPLICATE_ERROR("602", "不运行重复操作"),
    ILLEGAL_AGGREGATION_ERROR("603", "查询时应用的聚合函数不正确"),
    ILLEGAL_FILTER_ERROR("604", "查询时应用的过滤条件不正确"),
    STORAGE_OUT_OF_BOUND_ERROR("605", "性能模式存储，超出粒度容量限制，无法写入数据.(每毫秒一条数据)。请务必检查设备的存储聚合时间粒度设置！！！"),
    JSON_WRITE_ERROR("606", "写入数据库时，JSON序列化异常"),
    JSON_READ_ERROR("607", "读取数据库时，JSON反序列化异常"),
    DUPLICATE_TIME_ERROR("608", "兼容模式下，存在时间戳重复行。"),
    INSERT_VALUE_NUM_ERROR("609", "插入时，时间戳数量与数据行数量不匹配"),
    INSERT_MEASUREMENT_NUM_ERROR("609", "插入时，属性数量与数据列数量不匹配"),

    QUERY_AGGREGATION_TIME_ERROR("610", "查询聚合时间粒度不能小于存储聚合时间粒度"),
    INVALID_QUERY_AGGREGATION_TIME_ERROR("611", "不支持的查询聚合时间粒度"),
    INVALID_TAG_ERROR("612", "非法标识"),
    TAG_MEASUREMENT_CONFLICT("613", "标识属性冲突"),

    INVALID_ALERT_RULE_ERROR("620", "非法告警规则"),

    INVALID_STATUS_ERROR("701", "消息不能更新到该状态"),


    SYSTEM_ERROR("800", "系统异常"),
    DB_ERROR("801", "数据库异常"),
    IOT_DB_ERROR("802", "IOT数据库异常"),
    BEAN_ERROR("803", "Bean加载异常"),
    JSON_HANDEL_ERROR("804", "JSON处理异常"),
    STATE_MACHINE_ERROR("805", "状态机异常"),
    INSERT_DATA_ERROR("806", "插入数据异常"),

    FILE_HANDLER_ERROR("806", "文件处理异常"),
    UNSUPPORTED_FILE_TYPE_ERROR("807", "不支持的文件类型"),
    UNSUPPORTED_ALERT_CHANNEL_ERROR("808", "不支持的告警渠道"),
    FILE_EMPTY_ERROR("809", "文件为空"),
    DATA_FORMAT_ERROR("810", "数据格式异常"),
    TIME_FORMAT_ERROR("813", "时间格式处理异常, 或者不支持的时间格式"),
    TASK_CANCELLED("815", "任务已取消"),

    UPLOAD_DATA_FAILED("850", "上传数据失败"),
    DATA_CONVERSION_ERROR("851", "数据转换异常"),
    DATA_AGGREGATION_ERROR("852", "数据聚合异常"),

    AGGREGATE_QUERY_LIMIT_ERROR("860",
            "未使用快速聚合查询时，聚合查询的数据点超过配置的限制" +
                    "（请使用支持快速聚合查询的存储策略，或减少查询的数据点数，或增大配置文件中的限制）"),

    CONFIG_ERROR("870", "配置异常，请检查各个配置项，配置文件"),

    UNKNOWN_ERROR("900", "未知异常");

    public final String code;
    public final String msg;

    CodeMessage(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    @Override
    public String toString() {
        return "CodeMessage{" +
                "code='" + code + '\'' +
                ", msg='" + msg + '\'' +
                '}';
    }
}
