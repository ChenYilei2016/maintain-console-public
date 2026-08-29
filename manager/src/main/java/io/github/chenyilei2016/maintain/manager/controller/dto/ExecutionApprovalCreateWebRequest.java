package io.github.chenyilei2016.maintain.manager.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExecutionApprovalCreateWebRequest {
    @Valid
    @NotNull(message = "执行请求不能为空")
    private ExecutionTaskCreateWebRequest execution;
    @NotBlank(message = "申请理由不能为空")
    private String reason;
}
