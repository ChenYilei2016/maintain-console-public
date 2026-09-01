package io.github.chenyilei2016.maintain.manager.discovery;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.github.chenyilei2016.maintain.client.common.constants.MaintainConsoleClientCommonConst;
import io.github.chenyilei2016.maintain.manager.service.EnvironmentCatalogService;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author chenyilei
 * @since 2024/05/20 16:42
 */
@Component
@Profile("!local & !demo")
@ConditionalOnProperty(prefix = "maintain.manager.discovery", name = "mode", havingValue = "SPRING_CLOUD", matchIfMissing = true)
public class SpringCloudRegistryClientDiscovery implements MaintainConsoleRegistryClientDiscovery {

    /**
     * @see org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient
     */
    private final DiscoveryClient compositeDiscoveryClient;
    private final EnvironmentCatalogService environmentCatalogService;

    public SpringCloudRegistryClientDiscovery(
            DiscoveryClient compositeDiscoveryClient,
            EnvironmentCatalogService environmentCatalogService
    ) {
        this.compositeDiscoveryClient = compositeDiscoveryClient;
        this.environmentCatalogService = environmentCatalogService;
    }

    private static final String DUMMY = "DUMMY";


    @Data
    @AllArgsConstructor
    public static class ServiceInstanceCacheKey {
        private String serviceId;
        private String env;
    }

    private final LoadingCache<ServiceInstanceCacheKey, List<ServiceInstance>> serviceInstanceCache = CacheBuilder.newBuilder()
            .maximumSize(5000)
            .refreshAfterWrite(Duration.ofSeconds(30L))
            .build(new CacheLoader<ServiceInstanceCacheKey, List<ServiceInstance>>() {
                @Override
                public List<ServiceInstance> load(final ServiceInstanceCacheKey cacheKey) {
                    List<ServiceInstance> instances = compositeDiscoveryClient.getInstances(cacheKey.getServiceId());
                    if (CollectionUtils.isEmpty(instances)) {
                        return Collections.emptyList();
                    }
                    return instances.stream()
                            .filter(serviceInstance -> "true".equalsIgnoreCase(serviceInstance.getMetadata()
                                    .get(MaintainConsoleClientCommonConst.KEY_REGISTRY_ENABLED)))
                            .filter(serviceInstance -> isEnvMatch(cacheKey, serviceInstance))
                            .toList();
                }
            });

    private boolean isEnvMatch(ServiceInstanceCacheKey cacheKey, ServiceInstance serviceInstance) {
        String namespace = environmentCatalogService.namespace(cacheKey.getEnv());
        if (namespace == null) {
            return true;
        }
        return namespace.equalsIgnoreCase(
                serviceInstance.getMetadata().get(MaintainConsoleClientCommonConst.KEY_NAMESPACE));
    }

    private final LoadingCache<String, List<String>> serviceNamesCache = CacheBuilder.newBuilder()
            .maximumSize(5000)
            .refreshAfterWrite(Duration.ofSeconds(30L))
            .build(new CacheLoader<String, List<String>>() {
                @Override
                public List<String> load(String key) {
                    List<String> services = compositeDiscoveryClient.getServices();
                    if (CollectionUtils.isEmpty(services)) {
                        return Collections.emptyList();
                    }
                    return services;
                }
            });

    @Override
    public ServiceInstance findServiceInstance(String serviceName, String env) {
        List<ServiceInstance> serviceInstances = listServiceInstances(serviceName, env);
        if (serviceInstances.isEmpty()) {
            return null;
        }
        return serviceInstances.get(ThreadLocalRandom.current().nextInt(serviceInstances.size()));
    }

    @Override
    public List<ServiceInstance> listServiceInstances(String serviceName, String env) {
        if (!StringUtils.hasText(serviceName)) {
            throw new IllegalArgumentException("Service name must not be empty");
        }
        try {
            return List.copyOf(serviceInstanceCache.get(new ServiceInstanceCacheKey(serviceName, env)));
        } catch (ExecutionException e) {
            throw new IllegalStateException("读取服务实例失败", e.getCause());
        }
    }

    @Override
    public List<String> listServiceNames() {
        try {
            return serviceNamesCache.get(DUMMY);
        } catch (ExecutionException e) {
            throw new IllegalStateException("读取服务列表失败", e.getCause());
        }
    }
}
