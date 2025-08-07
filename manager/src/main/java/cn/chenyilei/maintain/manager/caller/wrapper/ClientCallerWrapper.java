package cn.chenyilei.maintain.manager.caller.wrapper;

import cn.chenyilei.maintain.client.common.dto.*;
import cn.chenyilei.maintain.manager.caller.ClientCaller;
import cn.chenyilei.maintain.manager.caller.ClientCallerContext;
import cn.chenyilei.maintain.manager.constant.ManagerConstants;
import cn.chenyilei.maintain.manager.discovery.MaintainConsoleRegistryClientDiscovery;
import cn.chenyilei.maintain.manager.utils.MutablePair;
import io.github.chenyilei2016.extension.spi.ExtensionLoader;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * @author chenyilei
 * @since 2024/05/21 14:53
 */
public class ClientCallerWrapper implements ClientCaller {

    private ClientCaller delegate;

    private final MaintainConsoleRegistryClientDiscovery registryClientDiscovery = ExtensionLoader.getExtensionLoader(MaintainConsoleRegistryClientDiscovery.class).getAdaptiveExtension();

    public ClientCallerWrapper(ClientCaller clientCaller) {
        this.delegate = clientCaller;
    }

    public <T> ApiResult<T> wrapProcess(ClientCallerContext ctx, Supplier<ApiResult<T>> runnable) {
        MutablePair<String, ServiceInstance> servicePair = ctx.getServiceInstancePair();
        String serviceName = servicePair.getKey();
        ServiceInstance serviceInstance = servicePair.getValue();
        if (serviceInstance != null) {
            return runnable.get();
        }
        if (StringUtils.isEmpty(serviceName)) {
            return ApiResult.error("Service instance name is empty");
        }
        //根据serviceName寻找服务实例
        serviceInstance = registryClientDiscovery.findServiceInstance(ctx, serviceName, ctx.getEnv());
        if (null == serviceInstance) {
            return ApiResult.error("无可用的服务实例");
        }
        ctx.setServiceInstance(serviceInstance);
        return runnable.get();
    }

    @Override
    public ApiResult<InvokeScriptResultDTO> $invokeScript(ClientCallerContext ctx, InvokeScriptParamSignDTO invokeScriptParamDTO) {
        ManagerConstants.grantSign(invokeScriptParamDTO);
        return wrapProcess(ctx, () -> delegate.$invokeScript(ctx, invokeScriptParamDTO));
    }

    @Override
    public ApiResult<InvokeCommandResultDTO> $invokeCommend(ClientCallerContext ctx, InvokeCommandParamSignDTO invokeCommandParamDTO) {
        ManagerConstants.grantSign(invokeCommandParamDTO);
        return wrapProcess(ctx, () -> delegate.$invokeCommend(ctx, invokeCommandParamDTO));
    }
}
