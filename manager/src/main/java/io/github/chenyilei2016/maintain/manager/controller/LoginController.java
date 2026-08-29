package io.github.chenyilei2016.maintain.manager.controller;

import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;
import io.github.chenyilei2016.maintain.manager.context.LoginUserContext;
import io.github.chenyilei2016.maintain.manager.controller.dto.res.LoginInfoWebResponse;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxResult;
import io.github.chenyilei2016.maintain.manager.service.EnvironmentCatalogService;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chenyilei
 * @email 705029004@qq.com
 * @since 2020/01/20- 22:55
 */
@RestController
public class LoginController {
    private final ManagerProperties managerProperties;
    private final EnvironmentCatalogService environmentCatalogService;
    private final Environment environment;

    public LoginController(
            ManagerProperties managerProperties,
            EnvironmentCatalogService environmentCatalogService,
            Environment environment
    ) {
        this.managerProperties = managerProperties;
        this.environmentCatalogService = environmentCatalogService;
        this.environment = environment;
    }

    @PostMapping("/manager/login/getInfo")
    public AjaxResult<LoginInfoWebResponse> getLoginInfo() {
        LoginInfoWebResponse r = new LoginInfoWebResponse();
        LocalLoginUser user = LoginUserContext.getUser();
        r.setEmployeeName(user.getEmployeeName());
        r.setEmployeeNo(user.getEmployeeNo());
        r.setCanApprove(user.getRoles().contains("ADMIN") || user.getRoles().contains("APPROVER")
                || managerProperties.getGlobalWhiteEmployeeNoList().contains(user.getEmployeeNo()));
        r.setAiEnabled(managerProperties.getAi().isEnabled());

        String[] activeProfiles = environment.getActiveProfiles();
        String currentEnv = activeProfiles.length == 0
                ? environment.getDefaultProfiles()[0]
                : activeProfiles[0];
        r.setEnv(currentEnv);
        r.setAvailableEnvironments(environmentCatalogService.list().stream().map(this::createEnvConfig).toList());

        return AjaxResult.success(r, "获取信息成功!");
    }

    /**
     * 创建环境配置项
     */
    private LoginInfoWebResponse.EnvironmentOption createEnvConfig(ManagerProperties.TargetEnvironment environment) {
        LoginInfoWebResponse.EnvironmentOption option = new LoginInfoWebResponse.EnvironmentOption();
        option.setValue(environment.getValue());
        option.setName(environment.getName());
        option.setIcon(environment.isProduction() ? "fas fa-circle text-red-500" : "fas fa-circle text-green-500");
        option.setCluster(environment.getCluster());
        option.setNamespace(environment.getNamespace());
        option.setDescription(environment.getDescription());
        option.setProduction(environment.isProduction());
        return option;
    }
}
