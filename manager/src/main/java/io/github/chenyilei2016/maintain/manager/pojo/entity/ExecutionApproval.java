package io.github.chenyilei2016.maintain.manager.pojo.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class ExecutionApproval {
    private String id;
    private String scriptId;
    private String scriptName;
    private String serviceName;
    private String environment;
    private ScriptTargetSelectionMode selectionMode;
    private String requestedInstanceId;
    private String requesterId;
    private String requesterName;
    private ExecutionApprovalStatus status;
    private String scriptContent;
    private String parameters;
    private String reason;
    private String approverId;
    private String approverName;
    private String decisionComment;
    private LocalDateTime createTime;
    private LocalDateTime expireTime;
    private LocalDateTime decisionTime;
    private LocalDateTime consumedTime;
    private String productionConfirmation;
}
