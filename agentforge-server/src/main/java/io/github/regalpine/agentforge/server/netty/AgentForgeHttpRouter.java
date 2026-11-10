package io.github.regalpine.agentforge.server.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * BE-07: Netty ChannelHandler that routes incoming HTTP requests
 * to registered path-prefix handlers.
 *
 * <p>Replaces {@code HttpServer.createContext()} route registration.
 * Handlers receive a {@link NettyRequestContext} adapter instead of
 * {@code HttpExchange}, providing equivalent functionality.
 */
public final class AgentForgeHttpRouter extends SimpleChannelInboundHandler<FullHttpRequest> {

    /**
     * Functional interface for Netty-based request handlers.
     */
    @FunctionalInterface
    public interface NettyHandler {
        void handle(NettyRequestContext ctx) throws Exception;
    }

    private final List<RouteEntry> routes = new ArrayList<>();

    /**
     * Register a handler for a path prefix.
     * Matches any request whose path starts with the given prefix.
     */
    public AgentForgeHttpRouter route(String pathPrefix, NettyHandler handler) {
        routes.add(new RouteEntry(pathPrefix, handler));
        return this;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        String path = request.uri().split("\\?")[0];
        NettyRequestContext reqCtx = new NettyRequestContext(request, ctx);

        for (RouteEntry route : routes) {
            if (path.startsWith(route.prefix)) {
                try {
                    route.handler.handle(reqCtx);
                } catch (Exception e) {
                    reqCtx.sendJson(500,
                        "{\"code\":\"INTERNAL_ERROR\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
                return;
            }
        }

        // No route matched → 404
        reqCtx.sendJson(404, "{\"code\":\"NOT_FOUND\",\"message\":\"Not found\"}");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.close();
    }

    private static String escapeJson(String s) {
        if (s == null) return "null";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    private record RouteEntry(String prefix, NettyHandler handler) {
    }
}
