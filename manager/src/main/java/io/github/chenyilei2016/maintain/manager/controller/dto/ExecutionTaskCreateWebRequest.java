package io.github.chenyilei2016.maintain.manager.controller.dto;

import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptTargetSelectionMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExecutionTaskCreateWebRequest {
    @NotBlank(message = "env环境不能为空")
    private String env;
    @NotBlank(message = "service不能为空")
    private String service;
    @NotBlank(message = "scriptId不能为空")
    private String scriptId;
    @NotBlank(message = "script脚本不能为空")
    private String script;
    private String params;
    private String parameterSchema;
    private ScriptTargetSelectionMode selectionMode = ScriptTargetSelectionMode.RANDOM;
    private String instanceId;
    @Min(value = 1, message = "超时时间不能小于 1 秒")
    @Max(value = 900, message = "超时时间不能超过 900 秒")
    private Integer timeoutSeconds;
    private String approvalId;
    private String productionConfirmation;
}
