package top.nomelin.iot.controller;

import cn.hutool.core.util.ObjectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.nomelin.iot.common.Result;
import top.nomelin.iot.common.enums.CodeMessage;
import top.nomelin.iot.common.exception.BusinessException;
import top.nomelin.iot.common.exception.SystemException;

/**
 * @author nomelin
 */
@RestControllerAdvice
public class ExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ExceptionHandler.class);
    @Value("${system.env}")
    private String env;

    /**
     * 处理业务异常
     *
     * @param e 业务异常
     * @return 结果
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(BusinessException.class)
    public Result doBusinessException(BusinessException e) {
        if ("dev".equals(env)) {// 开发环境才显示业务异常信息
            String cause = e.getCause() == null ? "" : e.getCause().getMessage();
            if (ObjectUtil.isEmpty(cause)) {
                cause = e.getCause() == null ? "" : e.getCause().toString();
            }
            log.warn("业务异常->" + e.codeMessage.code + ":" + e.codeMessage.msg +
                    ", cause:" + cause + ", extraMsg:" + e.extraMessage);
            e.printStackTrace();
            System.out.println("---test---");
            if (e.cause != null) {
                e.cause.printStackTrace();
            }
        }
        return new Result(e.codeMessage.code, e.codeMessage.msg);
    }

    /**
     * 处理系统异常
     *
     * @param e 系统异常
     * @return 结果
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(SystemException.class)
    public Result doSystemException(SystemException e) {
        String cause = e.getCause() == null ? "" : e.getCause().getMessage();
        if (ObjectUtil.isEmpty(cause)) {
            cause = e.getCause() == null ? "" : e.getCause().toString();
        }
        log.warn("系统异常->" + e.codeMessage.code + ":" + e.codeMessage.msg +
                ", cause:" + cause + ", extraMsg:" + e.extraMessage);
        e.printStackTrace();
        System.out.println("---test---");
        if (e.cause != null) {
            e.cause.printStackTrace();
        }
        return new Result(e.codeMessage.code, e.codeMessage.msg);
    }

    /**
     * 处理其它异常
     *
     * @param e 其它异常
     * @return 结果
     */
    @org.springframework.web.bind.annotation.ExceptionHandler(Throwable.class)
    public Result doException(Throwable e) {
        log.warn("其它异常->" + e.getMessage());
        e.printStackTrace();
        return new Result(CodeMessage.UNKNOWN_ERROR.code, CodeMessage.UNKNOWN_ERROR.msg);
    }
}
