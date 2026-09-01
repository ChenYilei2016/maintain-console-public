package io.github.chenyilei2016.maintain.manager.tools;

import com.alibaba.fastjson2.JSON;
import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import io.github.chenyilei2016.maintain.manager.constant.ScriptPermissionEnum;
import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;
import io.github.chenyilei2016.maintain.manager.discovery.MaintainConsoleRegistryClientDiscovery;
import io.github.chenyilei2016.maintain.manager.exceptions.CommonException;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptParameterSchema;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptPermissionEntity;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptToolMetadata;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ServiceInstanceDTO;
import io.github.chenyilei2016.maintain.manager.pojo.repository.ScriptRepository;
import io.github.chenyilei2016.maintain.manager.pojo.repository.ScriptRevisionRepository;
import io.github.chenyilei2016.maintain.manager.service.AuditLogService;
import io.github.chenyilei2016.maintain.manager.service.EnvironmentCatalogService;
import io.github.chenyilei2016.maintain.manager.service.ScriptAccessControl;
import io.github.chenyilei2016.maintain.manager.service.ScriptUserPreferenceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ToolService {
    private final ScriptAccessControl access;
    private final ScriptRepository scripts;
    private final ScriptRevisionRepository revisions;
    private final ScriptUserPreferenceService preferences;
    private final EnvironmentCatalogService environments;
    private final MaintainConsoleRegistryClientDiscovery discovery;
    private final ManagerProperties properties;
    private final AuditLogService audit;

    public ToolForm open(String scriptId, LocalLoginUser actor) {
        var tool = access.require(scriptId, actor, ScriptPermissionEnum.INVOKE);
        var grants = ScriptPermissionEntity.parse(tool.getScriptPermissions());
        if (!grants.isEnabled()) throw CommonException.createReminderException("工具已停用");
        ScriptParameterSchema schema = ScriptParameterSchema.parse(tool.getScript().getParameterSchema());
        if (schema == null) throw CommonException.createReminderException("此工具尚未完成类型化参数配置，请联系作者");
        schema.validateForScript(tool.getScriptContent());
        schema.getParameters().stream().filter(ScriptParameterSchema.ParameterDefinition::isSensitive)
                .forEach(parameter -> {
                    if (parameter.getDefaultValue() != null) parameter.setRequired(false);
                    parameter.setDefaultValue(null);
                    parameter.setExample(null);
                });
        List<EnvironmentChoice> choices = environments.list().stream()
                .filter(environment -> grants.getAllowedEnvironments() != null
                        && grants.getAllowedEnvironments().contains(environment.getValue()))
                .map(environment -> new EnvironmentChoice(environment.getValue(), environment.getName(), environment.isProduction()))
                .toList();
        preferences.touch(actor.getEmployeeNo(), scriptId);
        return new ToolForm(scriptId, tool.getDirectoryNode().getName(), tool.getScript().getDescription(),
                tool.getServiceName(), tool.getDirectoryNode().getCreatorName(), tool.getScript().getVersion(),
                ScriptToolMetadata.parse(tool.getScript().getToolMetadata()), schema.getParameters(), choices,
                grants.isAllowAllInstances(), access.allows(tool, actor, ScriptPermissionEnum.READ),
                access.allows(tool, actor, ScriptPermissionEnum.EDIT), properties.getExecution().getDefaultTimeoutSeconds());
    }

    public List<ServiceInstanceDTO> instances(String scriptId, String environment, LocalLoginUser actor) {
        var tool = access.require(scriptId, actor, ScriptPermissionEnum.INVOKE);
        var grants = ScriptPermissionEntity.parse(tool.getScriptPermissions());
        String canonicalEnvironment = environments.require(environment).getValue();
        if (!grants.allowsEnvironment(canonicalEnvironment, false)) {
            throw CommonException.createReminderException("工具未授权此环境或已停用");
        }
        return discovery.listServiceInstances(tool.getServiceName(), canonicalEnvironment).stream()
                .limit(properties.getExecution().getMaxTargets()).map(ServiceInstanceDTO::from).toList();
    }

    public List<ServiceInstanceDTO> developmentInstances(String scriptId, String environment, LocalLoginUser actor) {
        var tool = access.require(scriptId, actor, ScriptPermissionEnum.READ);
        var grants = ScriptPermissionEntity.parse(tool.getScriptPermissions());
        String canonicalEnvironment = environments.require(environment).getValue();
        if (!grants.allowsEnvironment(canonicalEnvironment, true)) {
            throw CommonException.createReminderException("工具未授权此环境或已停用");
        }
        return discovery.listServiceInstances(tool.getServiceName(), canonicalEnvironment).stream()
                .limit(properties.getExecution().getMaxTargets()).map(ServiceInstanceDTO::from).toList();
    }

    public GrantsView grants(String scriptId, LocalLoginUser actor) {
        var tool = access.require(scriptId, actor, ScriptPermissionEnum.MANAGE);
        return new GrantsView(tool.getScript().getVersion(), tool.getDirectoryNode().getCreatorId(),
                ScriptPermissionEntity.parse(tool.getScriptPermissions()));
    }

    @Transactional
    public int updateGrants(String scriptId, GrantChange request, LocalLoginUser actor) {
        var tool = access.require(scriptId, actor, ScriptPermissionEnum.MANAGE);
        if (!request.expectedVersion().equals(tool.getScript().getVersion())) {
            throw CommonException.createReminderException("授权设置已过期，请刷新后重新核对");
        }
        ScriptPermissionEntity grants = request.permissions();
        grants.setVersion(2);
        if (grants.getAllowedEnvironments() == null) throw new IllegalArgumentException("请明确设置允许环境");
        grants.setAllowedEnvironments(grants.getAllowedEnvironments().stream()
                .map(value -> environments.require(value).getValue()).distinct().toList());
        if (grants.getInvokerNo() != null && !grants.getInvokerNo().isBlank()) {
            var schema = ScriptParameterSchema.parse(tool.getScript().getParameterSchema());
            if (schema == null)
                throw CommonException.createReminderException("分享运行前请先配置类型化参数；旧原样替换只允许开发者调试");
            schema.validateForScript(tool.getScriptContent());
        }
        tool.getScript().setPermissions(JSON.toJSONString(grants)).setUpdateTime(LocalDateTime.now());
        var saved = scripts.save(tool.getScript(), true);
        if (saved == null) throw CommonException.createReminderException("授权保存冲突，请刷新并核对");
        revisions.saveRevision(saved, actor.getEmployeeNo(), actor.getEmployeeName());
        audit.record(actor, "TOOL_GRANTS_UPDATE", "SCRIPT", scriptId, "SUCCESS",
                Map.of("version", saved.getVersion(), "environments", grants.getAllowedEnvironments(), "enabled", grants.isEnabled()));
        return saved.getVersion();
    }

    public record EnvironmentChoice(String value, String name, boolean production) {
    }

    public record ToolForm(String id, String name, String description, String serviceName, String owner,
                           int version, ScriptToolMetadata metadata,
                           List<ScriptParameterSchema.ParameterDefinition> parameters,
                           List<EnvironmentChoice> environments, boolean allowAllInstances,
                           boolean canRead, boolean canEdit, int defaultTimeoutSeconds) {
    }

    public record GrantsView(int version, String ownerId, ScriptPermissionEntity permissions) {
    }

    public record GrantChange(@NotNull @Min(1) Integer expectedVersion,
                              @NotNull @Valid ScriptPermissionEntity permissions) {
    }
}
