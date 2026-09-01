package io.github.chenyilei2016.maintain.manager.controller.manager;

import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import io.github.chenyilei2016.maintain.manager.constant.ConsoleRole;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxResult;
import io.github.chenyilei2016.maintain.manager.security.RequireConsoleRole;
import io.github.chenyilei2016.maintain.manager.service.EnvironmentCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/manager/admin/environments")
@RequireConsoleRole(ConsoleRole.ADMIN)
@RequiredArgsConstructor
public class EnvironmentManagementController {
    private final ManagerProperties properties;
    private final EnvironmentCatalogService environments;

    @GetMapping
    public AjaxResult<Overview> overview() {
        List<EnvironmentView> targets = environments.list().stream().map(item -> new EnvironmentView(
                item.getValue(), item.getName(), item.isProduction(), item.getRegistryId(), item.getNamespace(),
                item.getGroupName(), item.getInstanceClusters() == null ? List.of() : List.copyOf(item.getInstanceClusters()))).toList();
        List<RegistryView> registries = properties.getDiscovery().getNacosConnections().stream().map(item -> new RegistryView(
                item.getId(), item.getName(), item.getNamespaceId(), item.getDefaultGroup(),
                item.getUsername() != null && !item.getUsername().isBlank())).toList();
        return AjaxResult.success(new Overview(properties.getDiscovery().getMode().name(), targets, registries));
    }

    public record Overview(String mode, List<EnvironmentView> environments, List<RegistryView> registries) {
    }

    public record EnvironmentView(String id, String name, boolean production, String registryId, String legacyNamespace,
                                  String groupName, List<String> instanceClusters) {
    }

    public record RegistryView(String id, String name, String namespaceId, String defaultGroup,
                               boolean authenticationConfigured) {
    }
}
