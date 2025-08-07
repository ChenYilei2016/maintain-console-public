package cn.chenyilei.maintain.manager.pojo.dto;

import lombok.Data;

/**
 * 脚本保存请求DTO
 *
 * @author chenyilei
 */
@Data
public class ScriptSaveRequestDTO {
    /**
     * 脚本ID（新建时为null）
     */
    private String scriptId;

    /**
     * 脚本名称
     */
    private String name;

    /**
     * 脚本内容
     */
    private String content;

    /**
     * 父文件夹ID
     */
    private String parentId;

    /**
     * 应用服务名
     */
    private String serviceName;

    /**
     * 权限配置JSON
     */
    private String permissions;

    /**
     * 目录类型: my-我的脚本, shared-共享脚本
     */
    private String directoryType;
}