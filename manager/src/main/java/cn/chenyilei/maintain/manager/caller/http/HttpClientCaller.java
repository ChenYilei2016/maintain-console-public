package cn.chenyilei.maintain.manager.caller.http;

import cn.chenyilei.maintain.client.common.dto.*;
import cn.chenyilei.maintain.manager.caller.ClientCaller;
import cn.chenyilei.maintain.manager.caller.ClientCallerContext;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import retrofit2.Call;

import java.io.IOException;
import java.net.URI;

/**
 * @author chenyilei
 * @since 2024/05/20 17:28
 */
public class HttpClientCaller implements ClientCaller {

    private final HttpMaintainConsoleClientApiAdapter httpMaintainConsoleClientApiAdapter = RetrofitHttpProxyFactory.getProxy(HttpMaintainConsoleClientApiAdapter.class);


    private String getHttpUrlFromServiceInstance(ServiceInstance serviceInstance) {
        return LoadBalancerUriTools.reconstructURI(serviceInstance, URI.create("")).toString();
    }

    private <T> T extractResult(Call<T> call) {
        try {
            return call.execute().body();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ApiResult<InvokeScriptResultDTO> $invokeScript(ClientCallerContext ctx, InvokeScriptParamSignDTO invokeScriptParamDTO) {
        Call<ApiResult<InvokeScriptResultDTO>> apiResultCall = httpMaintainConsoleClientApiAdapter
                .$invokeScript(getHttpUrlFromServiceInstance(ctx.getServiceInstancePair().getValue()), invokeScriptParamDTO);
        return extractResult(apiResultCall);
    }

    @Override
    public ApiResult<InvokeCommandResultDTO> $invokeCommend(ClientCallerContext ctx, InvokeCommandParamSignDTO invokeCommandParamDTO) {
        Call<ApiResult<InvokeCommandResultDTO>> apiResultCall = httpMaintainConsoleClientApiAdapter
                .$invokeCommend(getHttpUrlFromServiceInstance(ctx.getServiceInstancePair().getValue()), invokeCommandParamDTO);
        return extractResult(apiResultCall);
    }
}
