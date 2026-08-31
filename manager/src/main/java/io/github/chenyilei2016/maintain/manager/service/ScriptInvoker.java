package io.github.chenyilei2016.maintain.manager.service;

import io.github.chenyilei2016.maintain.client.common.console.IMaintainConsoleExecutor;
import io.github.chenyilei2016.maintain.client.common.dto.ApiResult;
import io.github.chenyilei2016.maintain.client.common.dto.InvokeScriptParamSignDTO;
import io.github.chenyilei2016.maintain.client.common.dto.InvokeScriptResultDTO;
import io.github.chenyilei2016.maintain.manager.caller.ClientCaller;
import io.github.chenyilei2016.maintain.manager.caller.ClientCallerContext;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptParameterSchema;
import io.github.chenyilei2016.maintain.manager.utils.MyProfileUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ScriptInvoker {
    private final ClientCaller clientCaller;
    private final ObjectProvider<IMaintainConsoleExecutor> localExecutorProvider;
    private final Environment environment;

    public ScriptInvoker(
            ClientCaller clientCaller,
            ObjectProvider<IMaintainConsoleExecutor> localExecutorProvider,
            Environment environment
    ) {
        this.clientCaller = clientCaller;
        this.localExecutorProvider = localExecutorProvider;
        this.environment = environment;
    }

    public ApiResult<InvokeScriptResultDTO> invoke(
            String serviceName,
            String targetEnvironment,
            ServiceInstance serviceInstance,
            String script,
            ScriptParameterSchema.ResolvedScript resolvedScript,
            long timeoutMillis
    ) {
        ApiResult<InvokeScriptResultDTO> result;
        if (MyProfileUtils.isLocal(environment)) {
            InvokeScriptResultDTO localResult = new InvokeScriptResultDTO();
            try {
                localResult.setScriptResult(Objects.toString(getLocalExecutor().execute(script)));
            } catch (RuntimeException failure) {
                return ApiResult.error(failure.getMessage());
            }
            result = ApiResult.success(localResult);
        } else {
            ClientCallerContext context = new ClientCallerContext(serviceName);
            context.setEnv(targetEnvironment);
            context.setServiceInstance(serviceInstance);
            context.setTimeoutMillis(timeoutMillis);
            result = clientCaller.$invokeScript(context, new InvokeScriptParamSignDTO(script));
        }
        if (result == null) throw new IllegalStateException("客户端未返回执行结果");
        if (!result.isSuccess()) {
            return result;
        }
        if (result.getData() == null) {
            throw new IllegalStateException("客户端返回空执行结果，无法确定远端状态");
        }
        result.getData().setScriptResult(resolvedScript.sanitizeResult(result.getData().getScriptResult()));
        return result;
    }

    public IMaintainConsoleExecutor getLocalExecutor() {
        IMaintainConsoleExecutor executor = localExecutorProvider.getIfAvailable();
        if (executor == null) {
            throw new IllegalStateException("本地脚本执行器未启用");
        }
        return executor;
    }
}
