package top.nomelin.iot.common.exception;


import top.nomelin.iot.common.enums.CodeMessage;

public class BusinessException extends RuntimeException {
    public final CodeMessage codeMessage;

    public final Throwable cause;

    public BusinessException(CodeMessage codeMessage, Throwable cause) {
        this.codeMessage = codeMessage;
        this.cause = cause;
    }

    public BusinessException(CodeMessage codeMessage) {
        this.codeMessage = codeMessage;
        this.cause = null;
    }


}
