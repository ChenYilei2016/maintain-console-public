package io.github.chenyilei2016.maintain.manager.service;

import io.github.chenyilei2016.maintain.client.common.dto.ApiResult;
import io.github.chenyilei2016.maintain.client.common.dto.RuntimeMetadataDTO;
import io.github.chenyilei2016.maintain.client.common.dto.RuntimeMetadataParamSignDTO;
import io.github.chenyilei2016.maintain.manager.caller.ClientCaller;
import io.github.chenyilei2016.maintain.manager.caller.ClientCallerContext;
import io.github.chenyilei2016.maintain.manager.discovery.MaintainConsoleRegistryClientDiscovery;
import io.github.chenyilei2016.maintain.manager.exceptions.CommonException;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ServiceInstanceDTO;
import io.github.chenyilei2016.maintain.manager.utils.MyProfileUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RuntimeMetadataService {
    private final MaintainConsoleRegistryClientDiscovery registryClientDiscovery;
    private final ClientCaller clientCaller;
    private final ScriptInvoker scriptInvoker;
    private final Environment environment;

    public RuntimeMetadataService(
            MaintainConsoleRegistryClientDiscovery registryClientDiscovery,
            ClientCaller clientCaller,
            ScriptInvoker scriptInvoker,
            Environment environment
    ) {
        this.registryClientDiscovery = registryClientDiscovery;
        this.clientCaller = clientCaller;
        this.scriptInvoker = scriptInvoker;
        this.environment = environment;
    }

    public RuntimeMetadataDTO load(String serviceName, String targetEnvironment, String instanceId) {
        if (MyProfileUtils.isLocal(environment)) {
            return scriptInvoker.getLocalExecutor().runtimeMetadata();
        }
        List<ServiceInstance> instances = registryClientDiscovery.listServiceInstances(serviceName, targetEnvironment);
        ServiceInstance instance = instanceId == null || instanceId.isBlank()
                ? instances.stream().findFirst().orElse(null)
                : instances.stream()
                .filter(candidate -> ServiceInstanceDTO.idOf(candidate).equals(instanceId))
                .findFirst().orElse(null);
        if (instance == null) {
            throw CommonException.createReminderException("无可用的目标实例");
        }
        ClientCallerContext context = new ClientCallerContext(serviceName);
        context.setEnv(targetEnvironment);
        context.setServiceInstance(instance);
        ApiResult<RuntimeMetadataDTO> result = clientCaller.$runtimeMetadata(context, new RuntimeMetadataParamSignDTO());
        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw CommonException.createReminderException(result == null ? "客户端未返回元数据" : result.getMsg());
        }
        return result.getData();
    }
}
