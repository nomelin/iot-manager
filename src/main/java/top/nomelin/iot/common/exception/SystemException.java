package top.nomelin.iot.common.exception;

import top.nomelin.iot.common.enums.CodeMessage;

public class SystemException extends RuntimeException {
    public final CodeMessage codeMessage;

    public Throwable cause;

    public SystemException(CodeMessage codeMessage, Throwable cause) {
        this.codeMessage = codeMessage;
        this.cause = cause;
    }

    public SystemException(CodeMessage codeMessage) {
        this.codeMessage = codeMessage;
        this.cause = null;
    }

}
