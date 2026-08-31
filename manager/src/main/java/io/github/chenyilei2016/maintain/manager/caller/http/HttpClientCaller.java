package io.github.chenyilei2016.maintain.manager.caller.http;

import io.github.chenyilei2016.maintain.client.common.dto.*;
import io.github.chenyilei2016.maintain.manager.caller.ClientCaller;
import io.github.chenyilei2016.maintain.manager.caller.ClientCallerContext;
import io.github.chenyilei2016.maintain.manager.discovery.MaintainConsoleRegistryClientDiscovery;
import io.github.chenyilei2016.maintain.manager.security.RequestSigner;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.stereotype.Component;
import retrofit2.Call;

import java.io.IOException;
import java.net.URI;

/**
 * @author chenyilei
 * @since 2024/05/20 17:28
 */
@Component
public class HttpClientCaller implements ClientCaller {

    private final HttpMaintainConsoleClientApiAdapter httpMaintainConsoleClientApiAdapter = RetrofitHttpProxyFactory.getProxy(HttpMaintainConsoleClientApiAdapter.class);
    private final MaintainConsoleRegistryClientDiscovery registryClientDiscovery;
    private final RequestSigner requestSigner;

    public HttpClientCaller(MaintainConsoleRegistryClientDiscovery registryClientDiscovery, RequestSigner requestSigner) {
        this.registryClientDiscovery = registryClientDiscovery;
        this.requestSigner = requestSigner;
    }

    private String getHttpUrlFromServiceInstance(ServiceInstance serviceInstance) {
        return LoadBalancerUriTools.reconstructURI(serviceInstance, URI.create("")).toString();
    }

    private <T> T extractResult(Call<T> call) {
        try {
            var response = call.execute();
            if (!response.isSuccessful()) {
                throw new IOException("Client HTTP " + response.code() + "，未获得确定的执行结果");
            }
            return response.body();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ApiResult<InvokeScriptResultDTO> $invokeScript(ClientCallerContext ctx, InvokeScriptParamSignDTO invokeScriptParamDTO) {
        ApiResult<InvokeScriptResultDTO> unavailable = prepareServiceInstance(ctx);
        if (unavailable != null) {
            return unavailable;
        }
        requestSigner.sign(invokeScriptParamDTO, ctx.getServiceInstance());
        Call<ApiResult<InvokeScriptResultDTO>> apiResultCall = httpMaintainConsoleClientApiAdapter
                .$invokeScript(getHttpUrlFromServiceInstance(ctx.getServiceInstance()), invokeScriptParamDTO);
        apiResultCall.timeout().timeout(ctx.getTimeoutMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        return extractResult(apiResultCall);
    }

    @Override
    public ApiResult<InvokeCommandResultDTO> $invokeCommend(ClientCallerContext ctx, InvokeCommandParamSignDTO invokeCommandParamDTO) {
        ApiResult<InvokeCommandResultDTO> unavailable = prepareServiceInstance(ctx);
        if (unavailable != null) {
            return unavailable;
        }
        requestSigner.sign(invokeCommandParamDTO, ctx.getServiceInstance());
        Call<ApiResult<InvokeCommandResultDTO>> apiResultCall = httpMaintainConsoleClientApiAdapter
                .$invokeCommend(getHttpUrlFromServiceInstance(ctx.getServiceInstance()), invokeCommandParamDTO);
        return extractResult(apiResultCall);
    }

    @Override
    public ApiResult<RuntimeMetadataDTO> $runtimeMetadata(
            ClientCallerContext ctx,
            RuntimeMetadataParamSignDTO request
    ) {
        ApiResult<RuntimeMetadataDTO> unavailable = prepareServiceInstance(ctx);
        if (unavailable != null) {
            return unavailable;
        }
        requestSigner.sign(request, ctx.getServiceInstance());
        return extractResult(httpMaintainConsoleClientApiAdapter.$runtimeMetadata(
                getHttpUrlFromServiceInstance(ctx.getServiceInstance()), request));
    }

    private <T> ApiResult<T> prepareServiceInstance(ClientCallerContext context) {
        if (context.getServiceInstance() != null) {
            return null;
        }
        if (context.getServiceName() == null || context.getServiceName().isBlank()) {
            return ApiResult.error("Service instance name is empty");
        }
        ServiceInstance serviceInstance = registryClientDiscovery.findServiceInstance(context.getServiceName(), context.getEnv());
        if (serviceInstance == null) {
            return ApiResult.error("无可用的服务实例");
        }
        context.setServiceInstance(serviceInstance);
        return null;
    }
}
