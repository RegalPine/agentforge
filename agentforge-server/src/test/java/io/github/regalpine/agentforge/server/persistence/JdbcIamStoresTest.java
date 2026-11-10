package io.github.regalpine.agentforge.server.persistence;

import io.github.regalpine.agentforge.security.iam.*;
import io.github.regalpine.agentforge.security.iam.SessionStore.SessionRecord;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.time.*;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** H2 驱动的 JDBC IAM/会话存储契约测试；验证并发 bootstrap、跨实例读取和撤销传播。 */
class JdbcIamStoresTest {
    private JdbcDataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:test-" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        ds.setUser("sa");
        ds.setPassword("");
        this.dataSource = ds;
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE SCHEMA IF NOT EXISTS agentforge_server");
            s.execute("""
                CREATE TABLE agentforge_server.iam_bootstrap_lock (
                    lock_id INTEGER PRIMARY KEY
                )""");
            s.execute("""
                INSERT INTO agentforge_server.iam_bootstrap_lock(lock_id) VALUES (1)""");
            s.execute("""
                CREATE TABLE agentforge_server.iam_tenant (
                    tenant_id VARCHAR(128) PRIMARY KEY,
                    tenant_name VARCHAR(255) NOT NULL UNIQUE,
                    record_json TEXT NOT NULL
                )""");
            s.execute("""
                CREATE TABLE agentforge_server.iam_user (
                    user_id VARCHAR(128) PRIMARY KEY,
                    username VARCHAR(255) NOT NULL UNIQUE,
                    email VARCHAR(320),
                    external_subject VARCHAR(255),
                    external_issuer VARCHAR(512),
                    record_json TEXT NOT NULL,
                    UNIQUE (external_subject, external_issuer)
                )""");
            s.execute("""
                CREATE TABLE agentforge_server.iam_role (
                    role_id VARCHAR(128) PRIMARY KEY,
                    tenant_id VARCHAR(128) REFERENCES agentforge_server.iam_tenant(tenant_id) ON DELETE CASCADE,
                    record_json TEXT NOT NULL
                )""");
            s.execute("""
                CREATE TABLE agentforge_server.iam_role_assignment (
                    user_id VARCHAR(128) NOT NULL REFERENCES agentforge_server.iam_user(user_id) ON DELETE CASCADE,
                    role_id VARCHAR(128) NOT NULL REFERENCES agentforge_server.iam_role(role_id) ON DELETE CASCADE,
                    tenant_scope VARCHAR(128) NOT NULL,
                    PRIMARY KEY (user_id, role_id, tenant_scope)
                )""");
            s.execute("""
                CREATE TABLE agentforge_server.iam_session (
                    token_hash CHAR(64) PRIMARY KEY,
                    user_id VARCHAR(128) NOT NULL REFERENCES agentforge_server.iam_user(user_id) ON DELETE CASCADE,
                    tenant_id VARCHAR(128) NOT NULL REFERENCES agentforge_server.iam_tenant(tenant_id) ON DELETE CASCADE,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
                    revoked_at TIMESTAMP WITH TIME ZONE,
                    CHECK (expires_at > created_at)
                )""");
        }
    }

    @Test
    void bootstrapIsIdempotentAndConcurrent() throws Exception {
        var stores = new JdbcIamStores(dataSource);
        var first = stores.bootstrap("admin-pw");
        assertTrue(first.isFirstRun());
        assertEquals("default", first.defaultTenant().tenantId());
        assertEquals("admin", first.adminUser().username());

        // 第二次 bootstrap 不重置数据
        var second = stores.bootstrap("different-pw");
        assertFalse(second.isFirstRun());
        assertEquals(first.adminUser().userId(), second.adminUser().userId());

        // 并发 bootstrap：两个线程同时执行，都不抛异常
        var latch = new CountDownLatch(1);
        var errors = new AtomicInteger(0);
        for (int i = 0; i < 4; i++) {
            new Thread(() -> {
                try {
                    latch.await();
                    new JdbcIamStores(dataSource).bootstrap("concurrent-pw");
                } catch (Exception e) {
                    errors.incrementAndGet();
                }
            }).start();
        }
        latch.countDown();
        Thread.sleep(500);
        assertEquals(0, errors.get(), "Concurrent bootstrap should not throw");
    }

    @Test
    void crossInstanceReadsShareSameData() {
        var instanceA = new JdbcIamStores(dataSource);
        instanceA.bootstrap("pw");

        // 实例 B 读取同一数据库
        var instanceB = new JdbcIamStores(dataSource);
        assertTrue(instanceB.users().findById("user-admin").isPresent());
        assertTrue(instanceB.tenants().findById("default").isPresent());
        assertFalse(instanceB.roles().findGlobalRoles().isEmpty());

        // A 写入的用户 B 立即可见
        instanceA.users().save(new User("u1", "alice", "Alice", "a@b.c",
            User.UserType.HUMAN, User.UserStatus.ACTIVE, null, null, null,
            java.util.Set.of(), java.util.Map.of(), Instant.now(), Instant.now()));
        assertTrue(instanceB.users().findByUsername("alice").isPresent());
    }

    @Test
    void sessionStoreRoundTripAndRevocation() {
        var stores = new JdbcIamStores(dataSource);
        stores.bootstrap("pw");
        var sessionStore = new JdbcSessionStore(dataSource);
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 7);
        Instant now = Instant.parse("2026-06-01T00:00:00Z");

        var providerA = new SessionTokenProvider(stores.users(), stores.roles(), stores.tenants(),
            "default", sessionStore, key, Duration.ofMinutes(10), Clock.fixed(now, ZoneOffset.UTC));
        String token = providerA.createSession("user-admin", "default");

        // 实例 B 使用同一 sessionStore 验证
        var providerB = new SessionTokenProvider(stores.users(), stores.roles(), stores.tenants(),
            "default", sessionStore, key, Duration.ofMinutes(10), Clock.fixed(now.plusSeconds(1), ZoneOffset.UTC));
        assertInstanceOf(io.github.regalpine.agentforge.security.AuthenticationSuccess.class, providerB.validate(token));

        // A 撤销后 B 拒绝
        assertTrue(providerA.invalidate(token));
        assertInstanceOf(io.github.regalpine.agentforge.security.AuthenticationFailure.class, providerB.validate(token));
    }

    @Test
    void sessionExpiryIsEnforced() {
        var stores = new JdbcIamStores(dataSource);
        stores.bootstrap("pw");
        var sessionStore = new JdbcSessionStore(dataSource);
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 7);
        Instant start = Instant.parse("2026-06-01T00:00:00Z");

        var creator = new SessionTokenProvider(stores.users(), stores.roles(), stores.tenants(),
            "default", sessionStore, key, Duration.ofMinutes(5), Clock.fixed(start, ZoneOffset.UTC));
        String token = creator.createSession("user-admin", "default");

        // 过期前 1 秒仍有效
        var beforeExpiry = new SessionTokenProvider(stores.users(), stores.roles(), stores.tenants(),
            "default", sessionStore, key, Duration.ofMinutes(5), Clock.fixed(start.plusSeconds(299), ZoneOffset.UTC));
        assertInstanceOf(io.github.regalpine.agentforge.security.AuthenticationSuccess.class, beforeExpiry.validate(token));

        // 过期时刻无效
        var atExpiry = new SessionTokenProvider(stores.users(), stores.roles(), stores.tenants(),
            "default", sessionStore, key, Duration.ofMinutes(5), Clock.fixed(start.plusSeconds(300), ZoneOffset.UTC));
        assertInstanceOf(io.github.regalpine.agentforge.security.AuthenticationFailure.class, atExpiry.validate(token));
    }

    @Test
    void rebuildPreservesIdentityAndData() {
        var stores1 = new JdbcIamStores(dataSource);
        stores1.bootstrap("pw");
        var sessionStore1 = new JdbcSessionStore(dataSource);
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 7);
        Instant now = Instant.parse("2026-06-01T00:00:00Z");

        var provider1 = new SessionTokenProvider(stores1.users(), stores1.roles(), stores1.tenants(),
            "default", sessionStore1, key, Duration.ofHours(1), Clock.fixed(now, ZoneOffset.UTC));
        String token = provider1.createSession("user-admin", "default");

        // "重建"应用实例
        var stores2 = new JdbcIamStores(dataSource);
        var sessionStore2 = new JdbcSessionStore(dataSource);
        var provider2 = new SessionTokenProvider(stores2.users(), stores2.roles(), stores2.tenants(),
            "default", sessionStore2, key, Duration.ofHours(1), Clock.fixed(now.plusSeconds(60), ZoneOffset.UTC));

        // 重建后 token 仍有效
        assertInstanceOf(io.github.regalpine.agentforge.security.AuthenticationSuccess.class, provider2.validate(token));
        // 数据仍在
        assertTrue(stores2.users().findById("user-admin").isPresent());
    }
}
