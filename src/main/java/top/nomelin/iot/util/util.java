package top.nomelin.iot.util;

import top.nomelin.iot.common.Constants;

/**
 * util
 *
 * @author nomelin
 * @since 2025/01/27 22:58
 **/
public class util {
    public static String getDevicePath(int userId, int deviceId) {
        return Constants.DATABASE_PREFIX + userId + Constants.DEVICE_PREFIX + deviceId;
    }
}
