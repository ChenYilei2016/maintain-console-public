package io.github.chenyilei2016.maintain.manager.identity;

import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 独立账号尚无可登录管理员时，用启动配置创建首个管理员。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "maintain.manager.identity", name = "mode", havingValue = "LOCAL_PASSWORD", matchIfMissing = true)
public class InitialAdministratorRunner implements CommandLineRunner {
    private final ConsoleUserService users;
    private final ManagerProperties properties;

    @Override
    public void run(String... args) {
        if (users.hasActiveLocalAdministrator()) return;
        ManagerProperties.BootstrapAdmin bootstrap = properties.getBootstrapAdmin();
        if (bootstrap.getPassword() == null || bootstrap.getPassword().isBlank()) {
            throw new IllegalStateException("No active local administrator exists. Configure "
                    + "maintain.manager.bootstrap-admin.password (env MAINTAIN_ADMIN_INITIAL_PASSWORD) before startup.");
        }
        users.bootstrapLocalAdministrator(bootstrap.getUsername(), bootstrap.getDisplayName(), bootstrap.getPassword());
        log.info("Bootstrapped initial local administrator account: {}. Change its password after login.",
                bootstrap.getUsername());
    }
}
