package io.github.chenyilei2016.maintain.manager.service;

import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import io.github.chenyilei2016.maintain.manager.discovery.RegistryDiscoveryMode;
import io.github.chenyilei2016.maintain.manager.exceptions.CommonException;
import io.github.chenyilei2016.maintain.manager.utils.MyProfileUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class EnvironmentCatalogService {
    private final ManagerProperties managerProperties;
    private final Environment springEnvironment;

    public EnvironmentCatalogService(ManagerProperties managerProperties, Environment springEnvironment) {
        this.managerProperties = managerProperties;
        this.springEnvironment = springEnvironment;
    }

    @PostConstruct
    void validateConfiguration() {
        Set<String> values = new HashSet<>();
        for (ManagerProperties.TargetEnvironment environment : list()) {
            if (environment.getValue() == null || environment.getValue().isBlank()
                    || environment.getName() == null || environment.getName().isBlank()) {
                throw new IllegalStateException("目标环境 value 和 name 不能为空");
            }
            if (!values.add(environment.getValue().toLowerCase(Locale.ROOT))) {
                throw new IllegalStateException("目标环境 value 不能重复: " + environment.getValue());
            }
        }
        if (managerProperties.getDiscovery().getMode() == RegistryDiscoveryMode.MULTI_NACOS) {
            Set<String> registryIds = new HashSet<>();
            for (ManagerProperties.NacosConnection connection : managerProperties.getDiscovery().getNacosConnections()) {
                if (connection.getId() == null || connection.getId().isBlank() || !registryIds.add(connection.getId())) {
                    throw new IllegalStateException("多 Nacos 连接 id 不能为空且不能重复");
                }
                if (connection.getServerAddr() == null || connection.getServerAddr().isBlank()) {
                    throw new IllegalStateException("Nacos 连接缺少 serverAddr: " + connection.getId());
                }
            }
            if (registryIds.isEmpty()) throw new IllegalStateException("MULTI_NACOS 模式至少需要一个 Nacos 连接");
            for (ManagerProperties.TargetEnvironment environment : list()) {
                if (!registryIds.contains(environment.getRegistryId())) {
                    throw new IllegalStateException("环境引用了不存在的 Nacos 连接: " + environment.getValue());
                }
            }
        }
    }

    public List<ManagerProperties.TargetEnvironment> list() {
        List<ManagerProperties.TargetEnvironment> environments = managerProperties.getTargetEnvironments();
        if (environments == null || environments.isEmpty()) {
            ManagerProperties.TargetEnvironment fallback = new ManagerProperties.TargetEnvironment();
            fallback.setValue("random");
            fallback.setName("全部命名空间");
            fallback.setAllNamespaces(true);
            fallback.setProduction(MyProfileUtils.isProd(springEnvironment));
            return List.of(fallback);
        }
        return List.copyOf(environments);
    }

    public ManagerProperties.TargetEnvironment require(String value) {
        if (value == null || value.isBlank()) {
            throw CommonException.createReminderException("目标环境不能为空");
        }
        return list().stream()
                .filter(environment -> value.equalsIgnoreCase(environment.getValue()))
                .findFirst()
                .orElseThrow(() -> CommonException.createReminderException("目标环境未配置: {}", value));
    }

    public boolean isProduction(String value) {
        return require(value).isProduction();
    }

    public String namespace(String value) {
        ManagerProperties.TargetEnvironment environment = require(value);
        if (environment.isAllNamespaces()) {
            return null;
        }
        return environment.getNamespace() == null || environment.getNamespace().isBlank()
                ? environment.getValue() : environment.getNamespace();
    }
}
