package io.github.chenyilei2016.maintain.manager.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author chenyilei
 * @since 2025/07/31 12:50
 */
@Data
public class ScriptEvalPreviewWebRequest {

    private String params;

    private String parameterSchema;

    /**
     * 前端原始脚本内容(未替换)
     */
    @NotBlank(message = "脚本内容不能为空")
    private String script;
}
