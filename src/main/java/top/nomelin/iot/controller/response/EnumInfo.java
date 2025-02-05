package top.nomelin.iot.controller.response;

/**
 * EnumInfo
 *
 * @author nomelin
 * @since 2025/02/05 23:39
 **/
public class EnumInfo {
    private final String code;
    private final String name;
    private final String desc;

    public EnumInfo(String code, String name, String desc) {
        this.code = code;
        this.name = name;
        this.desc = desc;
    }

    // Getters
    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }
}
