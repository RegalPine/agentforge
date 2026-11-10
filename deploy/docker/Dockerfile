# ── Stage 1: Build ──
FROM docker.1ms.run/library/eclipse-temurin:17-jdk AS builder

WORKDIR /build
COPY . .
RUN ./mvnw clean package -DskipTests -q

# ── Stage 2: Runtime ──
FROM docker.1ms.run/library/eclipse-temurin:17-jre

LABEL maintainer="AgentForge <agentforge@regalpine.io>"
LABEL org.opencontainers.image.title="AgentForge Server"
LABEL org.opencontainers.image.description="AgentForge Server - Graph Orchestration Engine"
LABEL org.opencontainers.image.vendor="Regalpine"

WORKDIR /opt/agentforge

# Install curl for health check
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/agentforge-server/target/agentforge-server-*-shaded.jar app.jar

# Non-root user
RUN groupadd -r agentforge && useradd -r -g agentforge -u 10001 agentforge
USER 10001

# Health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:8080/health/live || exit 1

# JVM defaults (overridable via JAVA_OPTS env)
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
