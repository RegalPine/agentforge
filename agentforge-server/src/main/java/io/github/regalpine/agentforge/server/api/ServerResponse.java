package io.github.regalpine.agentforge.server.api;

public record ServerResponse(
    int statusCode,
    String contentType,
    Object body
) {

    public static ServerResponse ok(
        Object body
    ) {
        return new ServerResponse(
            200,
            "application/json",
            body
        );
    }

    public static ServerResponse created(
        Object body
    ) {
        return new ServerResponse(
            201,
            "application/json",
            body
        );
    }

    public static ServerResponse error(
        ApiError error
    ) {
        return new ServerResponse(
            mapStatus(error.code()),
            "application/json",
            error
        );
    }

    private static int mapStatus(
        String code
    ) {
        if (code == null) {
            return 500;
        }

        return switch (code) {
            case "AF-SEC-001", "AF-SEC-002" -> 401;
            case "AF-SEC-003", "AF-SEC-004" -> 403;
            case "AF-SRV-404" -> 404;
            case "AF-SRV-409" -> 409;
            default -> 500;
        };
    }
}
