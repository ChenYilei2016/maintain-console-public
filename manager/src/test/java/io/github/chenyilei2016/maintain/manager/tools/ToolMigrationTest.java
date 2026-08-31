package io.github.chenyilei2016.maintain.manager.tools;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;

import static org.junit.jupiter.api.Assertions.*;

class ToolMigrationTest {
    @TempDir
    Path directory;

    @Test
    void upgradesV5WithoutRewritingLegacyCodePermissionsOrHistory() throws Exception {
        String url = "jdbc:sqlite:" + directory.resolve("upgrade.sqlite");
        Flyway.configure().dataSource(url, null, null).locations("classpath:db/migration/sqlite").target("5").load().migrate();
        try (var connection = DriverManager.getConnection(url); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO mc_directory_node(id,name,type,service_name,creator_id,creator_name) VALUES('old','旧工具','script','service','owner','作者')");
            statement.executeUpdate("INSERT INTO mc_script(id,content,permissions) VALUES('old','return 1','{}')");
            statement.executeUpdate("INSERT INTO mc_script_execution_history(id,script_id,script_name,service_name,executor_id,executor_name,script_content,result,status,start_time) VALUES('history','old','旧工具','service','owner','作者','return 1','1','success',CURRENT_TIMESTAMP)");
        }
        assertEquals(1, Flyway.configure().dataSource(url, null, null).locations("classpath:db/migration/sqlite").load().migrate().migrationsExecuted);
        try (var connection = DriverManager.getConnection(url); var statement = connection.createStatement()) {
            try (var row = statement.executeQuery("SELECT content,permissions,tool_metadata FROM mc_script WHERE id='old'")) {
                assertTrue(row.next());
                assertEquals("return 1", row.getString("content"));
                assertEquals("{}", row.getString("permissions"));
                assertNull(row.getString("tool_metadata"));
            }
            try (var row = statement.executeQuery("SELECT script_content,result,outcome FROM mc_script_execution_history WHERE id='history'")) {
                assertTrue(row.next());
                assertEquals("return 1", row.getString("script_content"));
                assertEquals("1", row.getString("result"));
                assertNull(row.getString("outcome"));
            }
            try (var row = statement.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name IN ('mc_script_execution_task','mc_execution_approval')")) {
                assertTrue(row.next());
                assertEquals(2, row.getInt(1), "旧任务、审批表保留，不做破坏性迁移");
            }
        }
    }
}
