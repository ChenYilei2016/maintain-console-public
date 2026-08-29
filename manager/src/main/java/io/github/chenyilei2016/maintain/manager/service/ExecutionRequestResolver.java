package io.github.chenyilei2016.maintain.manager.service;

import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import io.github.chenyilei2016.maintain.manager.constant.ScriptPermissionEnum;
import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;
import io.github.chenyilei2016.maintain.manager.controller.dto.ExecutionTaskCreateWebRequest;
import io.github.chenyilei2016.maintain.manager.discovery.MaintainConsoleRegistryClientDiscovery;
import io.github.chenyilei2016.maintain.manager.exceptions.CommonException;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptParameterSchema;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptPermissionEntity;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptTargetSelectionMode;
import io.github.chenyilei2016.maintain.manager.pojo.vo.ScriptVO;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class ExecutionRequestResolver {
    private final ScriptContentService scriptContentService;
    private final MaintainConsoleRegistryClientDiscovery registryClientDiscovery;
    private final ManagerProperties managerProperties;
    private final EnvironmentCatalogService environmentCatalogService;

    public ExecutionRequestResolver(
            ScriptContentService scriptContentService,
            MaintainConsoleRegistryClientDiscovery registryClientDiscovery,
            ManagerProperties managerProperties,
            EnvironmentCatalogService environmentCatalogService
    ) {
        this.scriptContentService = scriptContentService;
        this.registryClientDiscovery = registryClientDiscovery;
        this.managerProperties = managerProperties;
        this.environmentCatalogService = environmentCatalogService;
    }

    public ResolvedExecution resolve(ExecutionTaskCreateWebRequest request, LocalLoginUser user) {
        environmentCatalogService.require(request.getEnv());
        ScriptVO scriptVO = scriptContentService.findById(request.getScriptId());
        if (scriptVO == null) {
            throw CommonException.createReminderException("脚本不存在或节点异常");
        }
        if (!Objects.equals(request.getService(), scriptVO.getServiceName())) {
            throw CommonException.createReminderException("脚本不属于此服务");
        }
        if (!ScriptPermissionEntity.checkPermission(scriptVO.getDirectoryNode(), scriptVO.getScript(),
                user.getEmployeeNo(), ScriptPermissionEnum.INVOKE,
                managerProperties.getGlobalWhiteEmployeeNoList())) {
            throw CommonException.createReminderException("没有权限执行此脚本");
        }

        String parameterSchema = request.getParameterSchema() == null
                ? scriptVO.getScript().getParameterSchema()
                : request.getParameterSchema();
        ScriptParameterSchema.ResolvedScript resolvedScript = ScriptVO.resolveParamScript(
                request.getScript(), request.getParams(), parameterSchema);
        ScriptTargetSelectionMode selectionMode = request.getSelectionMode() == null
                ? ScriptTargetSelectionMode.RANDOM
                : request.getSelectionMode();
        List<ServiceInstance> selectedInstances = selectionMode.select(
                registryClientDiscovery.listServiceInstances(request.getService(), request.getEnv()),
                request.getInstanceId(), managerProperties.getExecution().getMaxTargets());
        int timeoutSeconds = request.getTimeoutSeconds() == null
                ? managerProperties.getExecution().getDefaultTimeoutSeconds()
                : request.getTimeoutSeconds();
        if (timeoutSeconds > managerProperties.getExecution().getMaxTimeoutSeconds()) {
            throw CommonException.createReminderException("超时时间不能超过 {} 秒",
                    managerProperties.getExecution().getMaxTimeoutSeconds());
        }
        return new ResolvedExecution(scriptVO, resolvedScript, selectionMode, selectedInstances, timeoutSeconds);
    }

    public record ResolvedExecution(
            ScriptVO script,
            ScriptParameterSchema.ResolvedScript resolvedScript,
            ScriptTargetSelectionMode selectionMode,
            List<ServiceInstance> instances,
            int timeoutSeconds
    ) {
    }
}
