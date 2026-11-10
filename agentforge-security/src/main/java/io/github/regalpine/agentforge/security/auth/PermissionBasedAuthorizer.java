package io.github.regalpine.agentforge.security.auth;

import io.github.regalpine.agentforge.security.AuthorizationRequest;
import io.github.regalpine.agentforge.security.Authorizer;

public final class PermissionBasedAuthorizer
    implements Authorizer {

    @Override
    public boolean authorize(
        AuthorizationRequest request
    ) {
        if (request == null) {
            return false;
        }

        if (request.identity() == null) {
            return false;
        }

        return request.identity()
            .permissions()
            .contains(request.action());
    }
}
