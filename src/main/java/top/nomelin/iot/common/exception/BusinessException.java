package top.nomelin.iot.common.exception;


import top.nomelin.iot.common.enums.CodeMessage;

public class BusinessException extends RuntimeException {
    public final CodeMessage codeMessage;

    public final String extraMessage;


    public final Throwable cause;

    public BusinessException(CodeMessage codeMessage, String extraMessage) {
        this.codeMessage = codeMessage;
        this.extraMessage = extraMessage;
        this.cause = null;
    }

    public BusinessException(CodeMessage codeMessage, Throwable cause) {
        this.codeMessage = codeMessage;
        this.cause = cause;
        this.extraMessage = null;
    }

    public BusinessException(CodeMessage codeMessage) {
        this.codeMessage = codeMessage;
        this.cause = null;
        this.extraMessage = null;
    }


}
