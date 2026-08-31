package io.github.chenyilei2016.maintain.manager.controller.manager;

import io.github.chenyilei2016.maintain.client.common.dto.RuntimeMetadataDTO;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxResult;
import io.github.chenyilei2016.maintain.manager.service.RuntimeMetadataService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/manager/service")
public class RuntimeMetadataController {
    private final RuntimeMetadataService runtimeMetadataService;
    private final io.github.chenyilei2016.maintain.manager.service.ScriptAccessControl access;
    private final io.github.chenyilei2016.maintain.manager.service.EnvironmentCatalogService environments;
    private final io.github.chenyilei2016.maintain.manager.tools.ToolService tools;

    public RuntimeMetadataController(RuntimeMetadataService runtimeMetadataService,
                                     io.github.chenyilei2016.maintain.manager.service.ScriptAccessControl access,
                                     io.github.chenyilei2016.maintain.manager.service.EnvironmentCatalogService environments,
                                     io.github.chenyilei2016.maintain.manager.tools.ToolService tools) {
        this.runtimeMetadataService = runtimeMetadataService;
        this.access = access;
        this.environments = environments;
        this.tools = tools;
    }

    @GetMapping("/instances")
    public AjaxResult<java.util.List<io.github.chenyilei2016.maintain.manager.pojo.entity.ServiceInstanceDTO>> instances(
            @RequestParam @NotBlank String scriptId, @RequestParam @NotBlank String environment) {
        return AjaxResult.success(tools.developmentInstances(scriptId, environment,
                io.github.chenyilei2016.maintain.manager.context.LoginUserContext.getUser()));
    }

    @GetMapping("/runtime-metadata")
    public AjaxResult<RuntimeMetadataDTO> runtimeMetadata(
            @RequestParam @NotBlank String serviceName,
            @RequestParam @NotBlank String environment,
            @RequestParam @NotBlank String scriptId,
            @RequestParam(required = false) String instanceId
    ) {
        var tool = access.require(scriptId,
                io.github.chenyilei2016.maintain.manager.context.LoginUserContext.getUser().getEmployeeNo(),
                io.github.chenyilei2016.maintain.manager.constant.ScriptPermissionEnum.READ);
        var grants = io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptPermissionEntity.parse(tool.getScriptPermissions());
        if (!serviceName.equals(tool.getServiceName()) || !grants.allowsEnvironment(environments.require(environment).getValue(), true)) {
            throw new IllegalArgumentException("目标不在此脚本允许范围内");
        }
        return AjaxResult.success(runtimeMetadataService.load(tool.getServiceName(), environment, instanceId));
    }
}
