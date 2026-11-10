package io.github.regalpine.agentforge.server.service;

import io.github.regalpine.agentforge.security.AuthorizationRequest;
import io.github.regalpine.agentforge.security.SecurityContext;
import io.github.regalpine.agentforge.security.auth.PermissionBasedAuthorizer;

/** 服务入口独立校验权限；HTTP 校验不能替代业务边界。 */
final class ServiceAuthorization {
    private ServiceAuthorization() {}

    static void require(SecurityContext context, String permission) {
        if (context == null || context.tenant() == null || context.tenant().tenantId().isBlank()
                || !new PermissionBasedAuthorizer().authorize(new AuthorizationRequest(
                    context.identity(), context.tenant().tenantId(), "service", null, permission))) {
            throw new SecurityException("Access denied");
        }
    }
}
