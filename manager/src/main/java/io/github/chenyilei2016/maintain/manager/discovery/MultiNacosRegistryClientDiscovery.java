package io.github.chenyilei2016.maintain.manager.discovery;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import io.github.chenyilei2016.maintain.client.common.constants.MaintainConsoleClientCommonConst;
import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import io.github.chenyilei2016.maintain.manager.constant.ManagerConstants;
import io.github.chenyilei2016.maintain.manager.service.EnvironmentCatalogService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@Profile("!local & !demo")
@ConditionalOnProperty(prefix = "maintain.manager.discovery", name = "mode", havingValue = "MULTI_NACOS")
public class MultiNacosRegistryClientDiscovery implements MaintainConsoleRegistryClientDiscovery {
    private final ManagerProperties properties;
    private final EnvironmentCatalogService environments;
    private final Map<String, NamingService> clients = new LinkedHashMap<>();

    public MultiNacosRegistryClientDiscovery(ManagerProperties properties, EnvironmentCatalogService environments) {
        this.properties = properties;
        this.environments = environments;
    }

    @PostConstruct
    void initialize() throws NacosException {
        for (ManagerProperties.NacosConnection connection : properties.getDiscovery().getNacosConnections()) {
            Properties config = new Properties();
            config.setProperty(PropertyKeyConst.SERVER_ADDR, connection.getServerAddr());
            if (connection.getNamespaceId() != null)
                config.setProperty(PropertyKeyConst.NAMESPACE, connection.getNamespaceId());
            if (connection.getUsername() != null)
                config.setProperty(PropertyKeyConst.USERNAME, connection.getUsername());
            if (connection.getPassword() != null)
                config.setProperty(PropertyKeyConst.PASSWORD, connection.getPassword());
            clients.put(connection.getId(), NacosFactory.createNamingService(config));
        }
    }

    @PreDestroy
    void shutdown() {
        clients.forEach((id, client) -> {
            try {
                client.shutDown();
            } catch (NacosException exception) {
                log.warn("关闭 Nacos 客户端失败, registryId:{}, exception:{}", id, exception.getClass().getSimpleName());
            }
        });
    }

    @Override
    public ServiceInstance findServiceInstance(String serviceName, String environment) {
        List<ServiceInstance> instances = listServiceInstances(serviceName, environment);
        return instances.isEmpty() ? null : instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
    }

    @Override
    public List<ServiceInstance> listServiceInstances(String serviceName, String environment) {
        ManagerProperties.TargetEnvironment target = environments.require(environment);
        Binding binding = binding(target);
        try {
            return binding.client().selectInstances(serviceName, binding.groupName(), target.getInstanceClusters(), true, true)
                    .stream().filter(Instance::isEnabled)
                    .filter(instance -> "true".equalsIgnoreCase(instance.getMetadata()
                            .get(MaintainConsoleClientCommonConst.KEY_REGISTRY_ENABLED)))
                    .map(instance -> toServiceInstance(binding.registryId(), binding.groupName(), serviceName,
                            binding.secure(), instance))
                    .toList();
        } catch (NacosException exception) {
            throw new IllegalStateException("读取目标环境的 Nacos 实例失败: " + target.getValue(), exception);
        }
    }

    @Override
    public List<String> listServiceNames() {
        int limit = Math.max(1, Math.min(properties.getDiscovery().getMaxServices(), 5_000));
        Set<String> names = new TreeSet<>();
        Set<String> queriedBindings = new HashSet<>();
        for (ManagerProperties.TargetEnvironment target : environments.list()) {
            Binding binding = binding(target);
            if (!queriedBindings.add(binding.registryId() + '\n' + binding.groupName())) continue;
            try {
                names.addAll(binding.client().getServicesOfServer(1, limit, binding.groupName()).getData());
            } catch (NacosException exception) {
                log.warn("读取 Nacos 服务列表失败, registryId:{}, group:{}, exception:{}",
                        binding.registryId(), binding.groupName(), exception.getClass().getSimpleName());
            }
            if (names.size() >= limit) break;
        }
        return names.stream().limit(limit).toList();
    }

    Binding binding(ManagerProperties.TargetEnvironment target) {
        if (target.getRegistryId() == null || target.getRegistryId().isBlank()) {
            throw new IllegalStateException("MULTI_NACOS 环境缺少 registryId: " + target.getValue());
        }
        NamingService client = clients.get(target.getRegistryId());
        ManagerProperties.NacosConnection connection = properties.getDiscovery().getNacosConnections().stream()
                .filter(item -> target.getRegistryId().equals(item.getId())).findFirst()
                .orElseThrow(() -> new IllegalStateException("环境引用了不存在的 Nacos 连接: " + target.getRegistryId()));
        if (client == null) throw new IllegalStateException("Nacos 客户端尚未初始化: " + target.getRegistryId());
        String group = target.getGroupName() == null || target.getGroupName().isBlank()
                ? connection.getDefaultGroup() : target.getGroupName();
        if (group == null || group.isBlank()) group = "DEFAULT_GROUP";
        return new Binding(target.getRegistryId(), group, connection.isSecure(), client);
    }

    static ServiceInstance toServiceInstance(String registryId, String groupName, String serviceName,
                                             boolean secure, Instance instance) {
        String nativeId = instance.getInstanceId() == null || instance.getInstanceId().isBlank()
                ? instance.getIp() + ':' + instance.getPort() : instance.getInstanceId();
        DefaultServiceInstance result = new DefaultServiceInstance(registryId + ':' + nativeId, serviceName,
                instance.getIp(), instance.getPort(), secure);
        result.getMetadata().putAll(instance.getMetadata());
        result.getMetadata().put(ManagerConstants.METADATA_REGISTRY_ID, registryId);
        result.getMetadata().put(ManagerConstants.METADATA_NACOS_GROUP, groupName);
        return result;
    }

    record Binding(String registryId, String groupName, boolean secure, NamingService client) {
    }
}
