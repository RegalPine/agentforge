package io.github.regalpine.agentforge.security;

public final class PermissionDeniedException
    extends RuntimeException {

    private final String code;

    public PermissionDeniedException(
        String code,
        String message
    ) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
