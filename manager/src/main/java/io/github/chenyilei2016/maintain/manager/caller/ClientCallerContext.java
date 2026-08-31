package io.github.chenyilei2016.maintain.manager.caller;

import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.client.ServiceInstance;

/**
 * @author chenyilei
 * @since 2024/05/20 17:38
 */
public class ClientCallerContext {

    @Getter
    private final String serviceName;

    @Getter
    @Setter
    private ServiceInstance serviceInstance;

    @Setter
    @Getter
    private String env;

    @Getter
    @Setter
    private long timeoutMillis = 300_000;

    public ClientCallerContext(String serviceName) {
        this.serviceName = serviceName;
    }
}
