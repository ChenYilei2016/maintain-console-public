package io.github.chenyilei2016.maintain.manager.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 移动目录树节点；parentId 为空表示移动到服务根目录。
 */
@Data
public class TreeNodeMoveWebRequest {

    @NotBlank(message = "节点ID不能为空")
    private String nodeId;

    private String parentId;
}
