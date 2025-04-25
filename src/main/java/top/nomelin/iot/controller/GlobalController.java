package top.nomelin.iot.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.nomelin.iot.aspect.CacheAspect;
import top.nomelin.iot.aspect.SessionManagementAspect;
import top.nomelin.iot.common.Result;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;

/**
 * GlobalController
 *
 * @author nomelin
 * @since 2025/04/21
 **/
@RestController
@RequestMapping("/global")
public class GlobalController {
    private final SessionManagementAspect sessionManagementAspect;

    private final CacheAspect cacheAspect;

    public GlobalController(SessionManagementAspect sessionManagementAspect, CacheAspect cacheAspect) {
        this.sessionManagementAspect = sessionManagementAspect;
        this.cacheAspect = cacheAspect;
    }

    @RequestMapping("/getIoTDBRetry")
    public Result getIoTDBRetry() {
        return Result.success(sessionManagementAspect.getMaxRetries());
    }

    @RequestMapping("/setIoTDBRetry/{retryCount}")
    public Result setIoTDBRetry(@PathVariable("retryCount") int retryCount) {
        if (retryCount < 0) {
            throw new BusinessException(CodeMessage.PARAM_ERROR, "重试次数不能小于0");
        }
        sessionManagementAspect.setMaxRetries(retryCount);
        return Result.success();
    }

    @RequestMapping("/getCacheEnabled")
    public Result getCacheEnable() {
        return Result.success(cacheAspect.isCacheEnabled());
    }

    @RequestMapping("/setCacheEnabled/{enabled}")
    public Result setCacheEnabled(@PathVariable("enabled") boolean enabled) {
        cacheAspect.setCacheEnabled(enabled);
        return Result.success();
    }

}
