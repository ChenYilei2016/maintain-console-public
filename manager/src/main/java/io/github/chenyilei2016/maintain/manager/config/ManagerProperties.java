package io.github.chenyilei2016.maintain.manager.config;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;

/**
 * @author chenyilei
 * @since 2025/08/08 11:28
 */
@Getter
@Setter
@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "maintain.manager")
public class ManagerProperties {

    /**
     * 全局白名单
     */
    public HashSet<String> globalWhiteEmployeeNoList = new HashSet<String>() {{
        //默认工号 0,1有全局权限
        add("0");
        add("1");
    }};


}
