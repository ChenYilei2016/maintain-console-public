package io.github.chenyilei2016.maintain.manager.discovery;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.github.chenyilei2016.extension.spi.context.Lifecycle;
import io.github.chenyilei2016.extension.spi.kernel.URL;
import io.github.chenyilei2016.maintain.client.common.constants.MaintainConsoleClientCommonConst;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * @author chenyilei
 * @since 2024/05/20 16:42
 */
public class SpringCloudRegistryClientDiscovery implements MaintainConsoleRegistryClientDiscovery, Lifecycle {

    /**
     * @see org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient
     */
    @Setter
    private DiscoveryClient compositeDiscoveryClient;

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
                @NotNull
                @ParametersAreNonnullByDefault
                public List<ServiceInstance> load(final ServiceInstanceCacheKey cacheKey) {
                    List<ServiceInstance> instances = compositeDiscoveryClient.getInstances(cacheKey.getServiceId());
                    if (CollectionUtils.isEmpty(instances)) {
                        return Collections.emptyList();
                    }
                    return instances.stream().filter(serviceInstance -> {
                        if (
                            // 是注册中心启用的服务
                                ("true".equalsIgnoreCase(serviceInstance.getMetadata().get(MaintainConsoleClientCommonConst.KEY_REGISTRY_ENABLED)))
                                        // 是选中的服务
                                        && isEnvMatch(cacheKey, serviceInstance)
                        ) {
                            return true;
                        }
                        return false;
                    }).limit(3).collect(Collectors.toList());
                }
            });

    private static boolean isEnvMatch(ServiceInstanceCacheKey cacheKey, ServiceInstance serviceInstance) {
        String env = cacheKey.getEnv();
        if (env.equalsIgnoreCase("random")) {
            return true;
        }
        return env.equalsIgnoreCase(serviceInstance.getMetadata().get(MaintainConsoleClientCommonConst.KEY_NAMESPACE));
    }

    private final LoadingCache<String, List<String>> serviceNamesCache = CacheBuilder.newBuilder()
            .maximumSize(5000)
            .refreshAfterWrite(Duration.ofSeconds(30L))
            .build(new CacheLoader<String, List<String>>() {
                @Override
                @NotNull
                @ParametersAreNonnullByDefault
                public List<String> load(String key) {
                    List<String> services = compositeDiscoveryClient.getServices();
                    if (CollectionUtils.isEmpty(services)) {
                        return Collections.emptyList();
                    }
                    return services;
                }
            });

    @Override
    public ServiceInstance findServiceInstance(URL url, String serviceName, String env) {
        if (StringUtils.isEmpty(serviceName)) {
            throw new IllegalArgumentException("Service name must not be empty");
        }
        try {
            List<ServiceInstance> serviceInstances = serviceInstanceCache.get(new ServiceInstanceCacheKey(serviceName, env));
            if (CollectionUtils.isEmpty(serviceInstances)) {
                return null;
            }
            //随机取一个
            return serviceInstances.get(ThreadLocalRandom.current().nextInt(serviceInstances.size()));
        } catch (ExecutionException ignore) {
        }
        return null;
    }

    @Override
    @SneakyThrows
    public List<String> listServiceNames(URL url) {
        return serviceNamesCache.get(DUMMY);
    }

    @Override
    public void initialize() throws IllegalStateException {
        if (this.compositeDiscoveryClient == null) {
            throw new IllegalStateException("SpringCloudRegistryClientDiscovery 无可用的 org.springframework.cloud.client.discovery.DiscoveryClient");
        }
    }

    @Override
    public void start() throws IllegalStateException {

    }

    @Override
    public void destroy() throws IllegalStateException {

    }
}
