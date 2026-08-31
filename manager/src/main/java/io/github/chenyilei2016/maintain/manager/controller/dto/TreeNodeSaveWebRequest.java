package io.github.chenyilei2016.maintain.manager.controller.dto;

import io.github.chenyilei2016.maintain.manager.constant.TreeNodeTypeEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

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
     * @see TreeNodeTypeEnum
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
    @jakarta.validation.constraints.Size(max = 200)
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
    @jakarta.validation.constraints.Size(max = 1_048_576)
    private String content;

    /**
     * 类型化参数 Schema JSON；为空时保持旧版参数替换协议。
     */
    @jakarta.validation.constraints.Size(max = 262_144)
    private String parameterSchema;

    /**
     * 权限配置JSON（仅当nodeType为script时有效）
     */
    private String permissions;

    /**
     * 脚本描述（仅当nodeType为script时有效）
     */
    @jakarta.validation.constraints.Size(max = 4000)
    private String description;

    /**
     * 编辑开始时看到的版本；更新脚本时必填。
     */
    private Integer expectedVersion;

    @jakarta.validation.Valid
    private io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptToolMetadata toolMetadata;

    /**
     * 仅创建时使用；后续通过独立授权入口修改。
     */
    private java.util.List<String> allowedEnvironments;

    /**
     * 操作人ID（由Controller层设置）
     */
    private String operatorId;

    /**
     * 操作人姓名（由Controller层设置）
     */
    private String operatorName;
}
