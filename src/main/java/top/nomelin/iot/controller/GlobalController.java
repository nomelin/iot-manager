package top.nomelin.iot.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.nomelin.iot.aspect.CacheAspect;
import top.nomelin.iot.aspect.SessionManagementAspect;
import top.nomelin.iot.common.GlobalConfig;
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

    //TODO 统一配置管理中心。将分散到各处的系统配置、降级配置整合到一起，提供统一的管理、查询入口。

    private final SessionManagementAspect sessionManagementAspect;

    private final CacheAspect cacheAspect;

    private final GlobalConfig globalConfig;


    public GlobalController(SessionManagementAspect sessionManagementAspect, CacheAspect cacheAspect, GlobalConfig globalConfig) {
        this.sessionManagementAspect = sessionManagementAspect;
        this.cacheAspect = cacheAspect;
        this.globalConfig = globalConfig;
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

    @RequestMapping("/getFastAggregateEnabled")
    public Result getFastAggregateEnabled() {
        return Result.success(globalConfig.isTryFastAggregate());
    }

    @RequestMapping("/setFastAggregateEnabled/{enabled}")
    public Result setFastAggregateEnabled(@PathVariable("enabled") boolean enabled) {
        globalConfig.setTryFastAggregate(enabled);
        return Result.success();
    }

}
