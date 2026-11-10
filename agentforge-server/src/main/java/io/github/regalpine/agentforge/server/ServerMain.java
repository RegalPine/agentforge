package io.github.regalpine.agentforge.server;

/**
 * AgentForge Server 启动入口.
 *
 * <p>BE-08: 已迁移至 {@link AgentForgeApplication} Builder 模式。
 * 配置加载优先级：环境变量 → agentforge.properties → 默认值。
 *
 * <p>使用 JDK 内置 HttpServer 启动轻量级 REST 服务。
 * 默认端口 8080，可通过环境变量 {@code AGENTFORGE_SERVER_PORT} 或
 * 配置文件 {@code server.port} 覆盖。
 *
 * <p>向后兼容：原有的 {@code AGENTFORGE_PORT} 和 {@code AGENTFORGE_ADMIN_PASSWORD}
 * 环境变量仍然有效（通过 ConfigLoader 映射）。
 */
public final class ServerMain {

    private ServerMain() {
    }

    public static void main(String[] args) throws Exception {
        AgentForgeApplication app = AgentForgeApplication.builder()
            .build();

        app.start();

        // 保持主线程
        Thread.currentThread().join();
    }
}
