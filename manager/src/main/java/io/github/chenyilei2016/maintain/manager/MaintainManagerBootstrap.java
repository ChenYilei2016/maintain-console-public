package io.github.chenyilei2016.maintain.manager;

import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

@Slf4j
@SpringBootApplication
@MapperScan(basePackages = "io.github.chenyilei2016.maintain.manager.pojo.mapper")
@EnableDiscoveryClient
@EnableConfigurationProperties(value = ManagerProperties.class)
public class MaintainManagerBootstrap {

    public static void useLocalEnv() {
        System.setProperty("spring.profiles.active", "local");
        // 如果本地没有sqlite数据库, 则自动创建并执行初始化脚本
        try {
            Path dbPath = resolveSqliteDbPath();
            log.info("预读取SQLite数据库路径: {}", dbPath.toAbsolutePath());
            if (Files.exists(dbPath)) {
                log.info("检测到本地SQLite已存在: {}", dbPath.toAbsolutePath());
                return;
            }

            // 确保目录存在
            Files.createDirectories(dbPath.getParent());
            //创建空文件
            Files.createFile(dbPath);
            // 并初始化库结构
            initSqliteWithSchema(dbPath);
            log.info("已自动创建并初始化SQLite数据库: {}", dbPath.toAbsolutePath());
        } catch (Exception e) {
            log.error("自动创建本地SQLite数据库失败", e);
        }
    }

    public static void useProdEnv() {
        System.setProperty("spring.profiles.active", "prod");
    }

    public static void main(String[] args) {
        // 本地数据库 + 本地注册中心
        useLocalEnv();

        // 使用PROD配置文件, 默认需要补充nacos 和 数据的配置
//        useProdEnv();
        SpringApplication.run(MaintainManagerBootstrap.class, args);
        log.info(">>>>>>>>>>>>> cyl spring boot start success !!!!  <<<<<<<<<<<<<<<");
    }

    // ===== helper methods =====

    /**
     * 解析需要创建的SQLite文件路径。
     * 兼容从项目根目录运行与从manager模块目录运行两种情况。
     */
    private static Path resolveSqliteDbPath() {
        // 优先与 application-local.properties 中一致的路径
        // 1) 项目根目录运行: manager/src/main/resources/sqlite/maintain-manager.sqlite
        Path candidate1 = Paths.get("manager", "src", "main", "resources", "sqlite", "maintain-manager.sqlite");
        // 2) 模块目录运行: src/main/resources/sqlite/maintain-manager.sqlite
        Path candidate2 = Paths.get("src", "main", "resources", "sqlite", "maintain-manager.sqlite");

        if (Files.exists(candidate1.getParent())) return candidate1;
        if (Files.exists(candidate2.getParent())) return candidate2;

        // 兜底：尝试在当前工作目录下创建 src/main/resources/sqlite/
        return candidate2;
    }

    /**
     * 创建并执行数据库建表脚本。
     */
    private static void initSqliteWithSchema(Path dbPath) throws IOException, SQLException {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setDriverClass(org.sqlite.JDBC.class);
        dataSource.setUrl("jdbc:sqlite:" + dbPath.toAbsolutePath());
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        log.info("jdbc sqlite path : {}", dbPath.toAbsolutePath());
        String sql = loadSchemaSql();
        if (sql == null || sql.trim().isEmpty()) {
            log.warn("未找到目录管理初始化SQL脚本, 跳过执行。");
            // 仍然创建一个空库文件
            throw new RuntimeException("未找到目录管理初始化SQL脚本, 跳过执行");
        }
        jdbcTemplate.update(sql);
    }

    /**
     * 从 classpath 或者 docs 目录读取 SQL 脚本。
     */
    private static String loadSchemaSql() throws IOException {
        ResourceLoader loader = new DefaultResourceLoader();
        // 优先从 classpath 读取（若未来被放入 resources）
        String[] classpathLocations = new String[]{
                "classpath:directory_management_sqlite.sql",
                "classpath:docs/directory_management_sqlite.sql",
                "classpath:sql/sqlite/directory_management_sqlite.sql"
        };
        for (String loc : classpathLocations) {
            Resource r = loader.getResource(loc);
            if (r.exists()) {
                try (InputStream is = r.getInputStream()) {
                    return readAll(is);
                }
            }
        }
        // 其次从文件系统的 docs 目录查找（支持从任意子目录启动）
        Path p = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 6 && p != null; i++, p = p.getParent()) {
            Path candidate = p.resolve(Paths.get("docs", "directory_management_sqlite.sql"));
            if (Files.exists(candidate)) {
                return new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static String readAll(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }


}
