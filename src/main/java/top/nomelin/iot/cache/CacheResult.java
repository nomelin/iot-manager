package top.nomelin.iot.cache;

/**
 * CacheResult
 *
 * @author nomelin
 * @since 2025/03/13 11:18
 **/
public class CacheResult<T> {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CacheResult.class);
    private final T data;
    private final boolean hit;
    private final boolean success;
    private final String message;

    private CacheResult(T data, boolean hit, boolean success, String message) {
        this.data = data;
        this.hit = hit;
        this.success = success;
        this.message = message;

        if (!success) {
            log.warn("缓存操作失败: {}", message);
        }
    }

    /**
     * 缓存命中
     */
    public static <T> CacheResult<T> hit(T data) {
        return new CacheResult<>(data, true, true, "缓存命中");
    }

    /**
     * 缓存未命中
     */
    public static <T> CacheResult<T> miss() {
        return new CacheResult<>(null, false, true, "缓存未命中");
    }

    /**
     * 缓存操作失败
     */
    public static <T> CacheResult<T> failure(String message) {
        return new CacheResult<>(null, false, false, message);
    }

    /**
     * 缓存操作失败
     */
    public static <T> CacheResult<T> failure(Throwable ex) {
        return new CacheResult<>(null, false, false, ex.getMessage());
    }

    // Getters
    public T getData() {
        return data;
    }

    public boolean isHit() {
        return hit;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "CacheResult{" +
                "data=" + data +
                ", hit=" + hit +
                ", success=" + success +
                ", message='" + message + '\'' +
                '}';
    }
}
