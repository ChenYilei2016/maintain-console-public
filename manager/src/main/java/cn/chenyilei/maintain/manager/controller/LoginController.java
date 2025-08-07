package cn.chenyilei.maintain.manager.controller;

import cn.chenyilei.maintain.manager.context.ApplicationContextHolder;
import cn.chenyilei.maintain.manager.controller.dto.res.LoginInfoWebResponse;
import cn.chenyilei.maintain.manager.pojo.common.AjaxResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

/**
 * @author chenyilei
 * @email 705029004@qq.com
 * @since 2020/01/20- 22:55
 */
@RestController
public class LoginController {

    @PostMapping("/manager/login/getInfo")
    public AjaxResult<LoginInfoWebResponse> getLoginInfo() {
//        UserDO r = loginService.login(loginVO);
        //tmp 临时
        LoginInfoWebResponse r = new LoginInfoWebResponse();
        r.setEmployeeId("1");
        r.setEmployeeName("cyl");
        r.setEmployeeNo("1");

        String currentEnv =
//                "pre"
                ApplicationContextHolder.getEnvironment().getActiveProfiles()[0];
        r.setEnv(currentEnv);

        // 根据不同环境设置可用环境配置
        if ("project".equals(currentEnv) || "mysql".equalsIgnoreCase(currentEnv)) {
            // project环境: 8080 8081 8082 8083
            r.setAvailableEnvironments(Arrays.asList(
                    createEnvConfig("random", "random端口", "fas fa-circle text-green-500"),
                    createEnvConfig("8080", "project-8080", "fas fa-circle text-green-500"),
                    createEnvConfig("8081", "project-8081", "fas fa-circle text-green-500"),
                    createEnvConfig("8082", "project-8082", "fas fa-circle text-green-500"),
                    createEnvConfig("8083", "project-8083", "fas fa-circle text-green-500")

            ));
        } else if ("pre".equals(currentEnv)) {
            // pre环境: 8080 8081 8082
            r.setAvailableEnvironments(Arrays.asList(
                    createEnvConfig("random", "random端口", "fas fa-circle text-red-500"),
                    createEnvConfig("8080", "pre-8080", "fas fa-circle text-red-500"),
                    createEnvConfig("8081", "pre-8081", "fas fa-circle text-red-500"),
                    createEnvConfig("8082", "pre-8082", "fas fa-circle text-red-500")
            ));
        } else if ("prod".equals(currentEnv)) {
            // prod环境: 只有8080
            r.setAvailableEnvironments(Arrays.asList(
                    createEnvConfig("random", "random端口", "fas fa-circle text-red-500"),
                    createEnvConfig("8080", "生产环境", "fas fa-circle text-red-500")
            ));
        } else {
            // 默认配置（开发环境或其他）
            r.setAvailableEnvironments(Arrays.asList(
                    createEnvConfig("random", "random端口", "fas fa-circle text-green-500")
            ));
        }

        return AjaxResult.success(r, "获取信息成功!");
    }

    /**
     * 创建环境配置项
     */
    private LoginInfoWebResponse.EnvironmentOption createEnvConfig(String value, String name, String icon) {
        LoginInfoWebResponse.EnvironmentOption option = new LoginInfoWebResponse.EnvironmentOption();
        option.setValue(value);
        option.setName(name);
        option.setIcon(icon);
        return option;
    }
}