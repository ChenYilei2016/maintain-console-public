package io.github.chenyilei2016.maintain.manager.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExecutionApprovalDecisionWebRequest {
    private boolean approved;
    @NotBlank(message = "审批意见不能为空")
    private String comment;
}
