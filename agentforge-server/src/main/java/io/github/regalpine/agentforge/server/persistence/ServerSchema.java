package io.github.regalpine.agentforge.server.persistence;

import org.flywaydb.core.Flyway;
import javax.sql.DataSource;

/** 服务存储独立版本历史；不自动 baseline 未知的既有数据库。 */
public final class ServerSchema {
    private ServerSchema() {}
    public static void migrate(DataSource source) {
        Flyway.configure().dataSource(source)
            .locations("classpath:db/server-migration")
            .schemas("agentforge_server")
            .defaultSchema("agentforge_server")
            .table("agentforge_server_schema_history")
            .baselineOnMigrate(false)
            .load().migrate();
    }
}
