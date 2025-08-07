package io.github.chenyilei2016.maintain.manager.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 树节点删除请求
 *
 * @author chenyilei
 * @since 2025/07/31 22:35
 */
@Data
public class TreeNodeDeleteWebRequest {

    /**
     * 要删除的节点ID
     */
    @NotBlank(message = "节点ID不能为空")
    private String nodeId;

    /**
     * 是否强制删除（对于文件夹，如果包含子节点，是否强制删除）
     */
    private Boolean forceDelete = false;

    /**
     * 操作人ID（由Controller层设置）
     */
    private String operatorId;

    /**
     * 操作人姓名（由Controller层设置）
     */
    private String operatorName;
}
