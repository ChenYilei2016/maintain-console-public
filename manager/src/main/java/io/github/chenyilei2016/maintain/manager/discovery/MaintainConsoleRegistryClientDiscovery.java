package io.github.chenyilei2016.maintain.manager.discovery;

import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

/**
 * @author chenyilei
 * @since 2024/05/21 14:18
 */
public interface MaintainConsoleRegistryClientDiscovery {
    ServiceInstance findServiceInstance(String serviceName, String env);

    List<ServiceInstance> listServiceInstances(String serviceName, String env);

    List<String> listServiceNames();
}
