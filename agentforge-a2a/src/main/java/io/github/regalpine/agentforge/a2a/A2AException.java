package io.github.regalpine.agentforge.a2a;

/**
 * A2A runtime exception.
 * AF-ENHANCE-10 §52: Failure.
 */
public class A2AException
    extends RuntimeException {

    private final A2AErrorCode errorCode;

    public A2AException(
        String message,
        A2AErrorCode errorCode
    ) {
        super(message);
        this.errorCode = errorCode;
    }

    public A2AException(
        String message,
        A2AErrorCode errorCode,
        Throwable cause
    ) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public A2AErrorCode errorCode() {
        return errorCode;
    }
}
