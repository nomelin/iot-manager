package top.nomelin.iot.common.exception;

import top.nomelin.iot.common.enums.CodeMessage;

public class SystemException extends RuntimeException {
    public final CodeMessage codeMessage;

    public final String extraMessage;//在具体抛出异常的地方，可以添加额外的具体错误信息。只会在日志中打印。

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

    public SystemException(CodeMessage codeMessage, String extraMessage) {
        this.codeMessage = codeMessage;
        this.extraMessage = extraMessage;
        this.cause = null;
    }

    public SystemException(CodeMessage codeMessage) {
        this.codeMessage = codeMessage;
        this.cause = null;
        this.extraMessage = "";
    }

    @Override
    public String toString() {
        return "SystemException{" +
                "codeMessage=" + codeMessage +
                ", extraMessage='" + extraMessage + '\'' +
                ", cause=" + cause +
                '}';
    }
}
