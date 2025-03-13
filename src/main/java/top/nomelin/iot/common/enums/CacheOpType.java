package top.nomelin.iot.common.enums;

public enum CacheOpType {
    /**
     * 查缓存，如果未命中则继续执行方法，并将出参放入缓存。
     */
    GET,
    /**
     * 存入缓存：新增或更新缓存。
     */
    PUT,
    /**
     * 删除缓存。
     */
    DELETE
}
