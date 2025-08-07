package io.github.chenyilei2016.maintain.manager.caller;

import io.github.chenyilei2016.extension.spi.kernel.URL;
import io.github.chenyilei2016.maintain.manager.utils.MutablePair;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.client.ServiceInstance;

/**
 * @author chenyilei
 * @since 2024/05/20 17:38
 */
public class ClientCallerContext extends URL {

    public URL getExtensionUrl() {
        return this;
    }

    /**
     * 一个可用的pair ; serviceName => 服务实例
     */
    @Getter
    private final MutablePair<String, ServiceInstance> serviceInstancePair = new MutablePair<>();

    @Setter
    @Getter
    private String env;

    public ClientCallerContext(String serviceName) {
        this.getServiceInstancePair().setKey(serviceName);
    }

    public void setServiceInstance(ServiceInstance serviceInstance) {
        this.getServiceInstancePair().setValue(serviceInstance);
    }
}
