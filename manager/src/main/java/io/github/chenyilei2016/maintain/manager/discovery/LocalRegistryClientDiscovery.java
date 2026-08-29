package io.github.chenyilei2016.maintain.manager.discovery;

import io.github.chenyilei2016.maintain.manager.service.EnvironmentCatalogService;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.chenyilei2016.maintain.manager.CONST.APP_NAME;

/**
 * @author chenyilei
 * @since 2024/05/20 16:42
 */
@Component
@Profile("local")
public class LocalRegistryClientDiscovery implements MaintainConsoleRegistryClientDiscovery {
    private final EnvironmentCatalogService environmentCatalogService;
    private final Environment environment;

    public LocalRegistryClientDiscovery(
            EnvironmentCatalogService environmentCatalogService,
            Environment environment
    ) {
        this.environmentCatalogService = environmentCatalogService;
        this.environment = environment;
    }

    @Override
    public ServiceInstance findServiceInstance(String serviceName, String env) {
        return listServiceInstances(serviceName, env).getFirst();
    }

    @Override
    public List<ServiceInstance> listServiceInstances(String serviceName, String env) {
        environmentCatalogService.require(env);
        int port = Integer.parseInt(environment.getRequiredProperty("server.port"));
        return List.of(new DefaultServiceInstance(APP_NAME, APP_NAME, "127.0.0.1", port, false));
    }

    @Override
    public List<String> listServiceNames() {
        return List.of(APP_NAME, "MOCK SERVICE");
    }
}
