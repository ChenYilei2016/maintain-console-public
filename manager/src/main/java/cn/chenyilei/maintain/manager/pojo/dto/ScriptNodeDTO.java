package cn.chenyilei.maintain.manager.pojo.dto;

import lombok.Data;

/**
 * 目录节点DTO
 *
 * @author chenyilei
 */
@Data
public class ScriptNodeDTO {
    /**
     * 节点ID
     */
    private String id;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点类型: folder-文件夹, script-脚本
     */
    private String type;

    /**
     * 父节点ID
     */
    private String parentId;

    /**
     * 应用服务名
     */
    private String serviceName;

    /**
     * 脚本内容（仅当type为script时有值）
     */
    private String content;

    /**
     * 权限配置JSON
     */
    private String permissions;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * 修改时间
     */
    private String updateTime;

    /**
     * 权限类型: public-公共, private-私有
     */
    private String permissionType;


    private boolean canRead = true;

    private boolean canEdit;

    private boolean canInvoke;

}