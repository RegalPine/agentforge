package io.github.regalpine.agentforge.server.netty;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.FullHttpRequest;

import java.nio.charset.StandardCharsets;

/**
 * BE-07: Adapter wrapping Netty's FullHttpRequest/ChannelHandlerContext,
 * providing an interface equivalent to HttpExchange for existing handlers.
 *
 * <p>This allows existing handler business logic (routing, JSON construction,
 * error handling) to be preserved — only I/O operations are replaced.
 */
public final class NettyRequestContext {

    private final FullHttpRequest request;
    private final ChannelHandlerContext ctx;
    private final String path;
    private final String method;

    public NettyRequestContext(FullHttpRequest request, ChannelHandlerContext ctx) {
        this.request = request;
        this.ctx = ctx;
        this.path = request.uri().split("\\?")[0];
        this.method = request.method().name();
    }

    /**
     * HTTP method (GET, POST, PUT, DELETE).
     */
    public String method() {
        return method;
    }

    /**
     * Request path without query string.
     */
    public String path() {
        return path;
    }

    /**
     * Full URI including query string.
     */
    public String uri() {
        return request.uri();
    }

    /**
     * Get a request header value.
     */
    public String header(String name) {
        return request.headers().get(name);
    }

    /**
     * Request body as string.
     */
    public String body() {
        return request.content().toString(StandardCharsets.UTF_8);
    }

    /**
     * Send a JSON response.
     */
    public void sendJson(int status, String json) {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
            request.protocolVersion(),
            HttpResponseStatus.valueOf(status),
            Unpooled.wrappedBuffer(bytes)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        HttpUtil.setContentLength(response, bytes.length);
        ctx.writeAndFlush(response);
    }

    /**
     * Send a 200 OK JSON response.
     */
    public void sendJsonOk(String json) {
        sendJson(200, json);
    }

    /**
     * Send a 204 No Content response.
     */
    public void sendNoContent() {
        FullHttpResponse response = new DefaultFullHttpResponse(
            request.protocolVersion(),
            HttpResponseStatus.NO_CONTENT,
            Unpooled.EMPTY_BUFFER
        );
        ctx.writeAndFlush(response);
    }

    /**
     * Send an SSE event (text/event-stream).
     * BE-07: Netty native SSE support — writes directly to the channel.
     */
    public void sendSseEvent(String event, String data) {
        String ssePayload = "event: " + event + "\ndata: " + data + "\n\n";
        byte[] bytes = ssePayload.getBytes(StandardCharsets.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
            request.protocolVersion(),
            HttpResponseStatus.OK,
            Unpooled.wrappedBuffer(bytes)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
        ctx.writeAndFlush(response);
    }
}
