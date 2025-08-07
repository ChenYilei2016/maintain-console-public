package io.github.chenyilei2016.maintain.manager.discovery;

import io.github.chenyilei2016.extension.spi.ExtensionAdaptive;
import io.github.chenyilei2016.extension.spi.ExtensionSPI;
import io.github.chenyilei2016.extension.spi.kernel.URL;
import io.github.chenyilei2016.maintain.manager.constant.SPIConstants;
import org.springframework.cloud.client.ServiceInstance;

import java.util.List;

/**
 * @author chenyilei
 * @since 2024/05/21 14:18
 */
@ExtensionSPI("springCloud")
public interface MaintainConsoleRegistryClientDiscovery {
    @ExtensionAdaptive(value = {SPIConstants.REGISTRY_CLIENT_DISCOVERY_TYPE})
    ServiceInstance findServiceInstance(URL url, String serviceName, String env);

    @ExtensionAdaptive(value = {SPIConstants.REGISTRY_CLIENT_DISCOVERY_TYPE})
    List<String> listServiceNames(URL url);
}
