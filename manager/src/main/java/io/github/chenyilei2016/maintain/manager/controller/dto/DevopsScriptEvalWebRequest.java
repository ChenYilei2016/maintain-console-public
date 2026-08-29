package io.github.chenyilei2016.maintain.manager.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author chenyilei
 * @since 2026/04/03 16:32
 */
@Data
public class DevopsScriptEvalWebRequest {
    @NotBlank(message = "env环境不能为空")
    private String env;
    @NotBlank(message = "选择应用不能为空")
    private String service;
    @NotBlank(message = "脚本ID不能为空")
    private String scriptId;

    private String params;
}
