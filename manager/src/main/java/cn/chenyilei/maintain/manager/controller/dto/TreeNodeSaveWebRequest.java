package cn.chenyilei.maintain.manager.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 树节点保存请求
 *
 * @author chenyilei
 * @since 2025/07/31 22:35
 */
@Data
public class TreeNodeSaveWebRequest {

    /**
     * 节点类型
     *
     * @see cn.chenyilei.maintain.manager.constant.TreeNodeTypeEnum
     */
    @NotBlank(message = "节点类型不能为空")
    private String nodeType;

    /**
     * 节点ID，存在表示更新，不存在表示新增
     */
    private String nodeId;

    /**
     * 节点名称
     */
    @NotBlank(message = "节点名称不能为空")
    private String nodeName;

    /**
     * 父节点ID，null表示根节点
     */
    private String parentId;

    /**
     * 服务名称
     */
    @NotBlank(message = "服务名称不能为空")
    private String serviceName;

    /**
     * 脚本内容（仅当nodeType为script时有效）
     */
    private String content;

    /**
     * 权限配置JSON（仅当nodeType为script时有效）
     */
    private String permissions;

    /**
     * 脚本描述（仅当nodeType为script时有效）
     */
    private String description;

    /**
     * 操作人ID（由Controller层设置）
     */
    private String operatorId;

    /**
     * 操作人姓名（由Controller层设置）
     */
    private String operatorName;
}
