package io.github.chenyilei2016.maintain.manager.pojo.entity;

import io.github.chenyilei2016.maintain.client.common.constants.MaintainConsoleClientCommonConst;
import io.github.chenyilei2016.maintain.manager.constant.ManagerConstants;
import org.springframework.cloud.client.ServiceInstance;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record ServiceInstanceDTO(
        String id,
        String serviceId,
        String host,
        int port,
        boolean secure,
        String uri,
        Map<String, String> metadata
) {
    private static final Set<String> SAFE_METADATA_KEYS = Set.of(
            MaintainConsoleClientCommonConst.KEY_REGISTRY_ENABLED,
            MaintainConsoleClientCommonConst.KEY_REGISTRY_VERSION,
            MaintainConsoleClientCommonConst.KEY_NAMESPACE,
            ManagerConstants.METADATA_REGISTRY_ID,
            ManagerConstants.METADATA_NACOS_GROUP
    );

    public static ServiceInstanceDTO from(ServiceInstance instance) {
        return new ServiceInstanceDTO(idOf(instance), instance.getServiceId(), instance.getHost(), instance.getPort(),
                instance.isSecure(), instance.getUri().toString(), instance.getMetadata().entrySet().stream()
                .filter(entry -> SAFE_METADATA_KEYS.contains(entry.getKey()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    public static String idOf(ServiceInstance instance) {
        return instance.getInstanceId() == null || instance.getInstanceId().isBlank()
                ? instance.getHost() + ':' + instance.getPort()
                : instance.getInstanceId();
    }
}
