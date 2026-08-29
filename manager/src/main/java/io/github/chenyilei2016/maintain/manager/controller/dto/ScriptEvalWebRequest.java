package io.github.chenyilei2016.maintain.manager.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author chenyilei
 * @since 2025/07/31 12:50
 */
@Data
public class ScriptEvalWebRequest {
    @NotBlank(message = "env环境不能为空")
    private String env;
    @NotBlank(message = "选择应用不能为空")
    private String service;
    @NotBlank(message = "脚本ID不能为空")
    private String scriptId;

    private String params;

    /**
     * 当前编辑器中的参数 Schema；允许执行尚未保存的脚本版本。
     */
    private String parameterSchema;

    /**
     * 前端原始脚本内容(未替换)
     */
    @NotBlank(message = "脚本内容不能为空")
    private String script;

    private boolean debug = false;
}
