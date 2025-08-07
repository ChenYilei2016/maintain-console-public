package cn.chenyilei.maintain.manager.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

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
     * 前端原始脚本内容(未替换)
     */
    @NotBlank(message = "脚本内容不能为空")
    private String script;

    private boolean debug = false;
}
