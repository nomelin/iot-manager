package top.nomelin.iot.common.exception;

import top.nomelin.iot.common.enums.CodeMessage;

public class SystemException extends RuntimeException {
    public final CodeMessage codeMessage;

    public final String extraMessage;

    public final Throwable cause;

    public SystemException(CodeMessage codeMessage, String extraMessage, Throwable cause) {
        this.codeMessage = codeMessage;
        this.extraMessage = extraMessage;
        this.cause = cause;
    }

    public SystemException(CodeMessage codeMessage, Throwable cause) {
        this.codeMessage = codeMessage;
        this.cause = cause;
        this.extraMessage = "";
    }

    public SystemException(CodeMessage codeMessage) {
        this.codeMessage = codeMessage;
        this.cause = null;
        this.extraMessage = "";
    }

}
