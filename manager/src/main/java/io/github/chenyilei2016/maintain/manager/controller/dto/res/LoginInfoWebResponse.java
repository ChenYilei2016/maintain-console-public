package io.github.chenyilei2016.maintain.manager.controller.dto.res;

import lombok.Data;

import java.util.List;

/**
 * @author chenyilei
 * @since 2025/08/01 14:51
 */
@Data
public class LoginInfoWebResponse {
    /**
     * 员工姓名
     */
    private String employeeName;
    /**
     * 员工工号
     */
    private String employeeNo;
    private boolean canCreateTools;


    private String env;

    /**
     * 可用环境列表
     */
    private List<EnvironmentOption> availableEnvironments;


    private boolean aiEnabled;

    /**
     * 环境选项
     */
    @Data
    public static class EnvironmentOption {
        /**
         * 环境值
         */
        private String value;

        /**
         * 环境名称
         */
        private String name;

        /**
         * 图标样式
         */
        private String icon;

        private String cluster;

        private String namespace;

        private String description;

        private boolean production;
    }
}
