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


    USER_NAME_EXIST_ERROR("501", "用户名已存在"),
    USER_NOT_LOGIN_ERROR("502", "用户未登录"),
    USER_ACCOUNT_ERROR("503", "账号或密码错误"),
    USER_NOT_EXIST_ERROR("504", "用户不存在"),
    PARAM_PASSWORD_ERROR("505", "原密码输入错误"),
    USER_NAME_NULL_ERROR("506", "用户名缺失"),
    INVALID_USER_NAME_ERROR("507", "要操作的用户名不合法，无权限操作"),
    EQUAL_PASSWORD_ERROR("508", "新密码不能与原密码相同"),

    NO_PERMISSION_ERROR("601", "无权限"),


    SYSTEM_ERROR("800", "系统异常"),
    DB_ERROR("801", "数据库异常"),
    IOT_DB_ERROR("802", "IOT数据库异常"),
    BEAN_ERROR("803", "Bean加载异常"),

    UNKNOWN_ERROR("900", "未知异常"),
    ;

    public final String code;
    public final String msg;

    CodeMessage(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
