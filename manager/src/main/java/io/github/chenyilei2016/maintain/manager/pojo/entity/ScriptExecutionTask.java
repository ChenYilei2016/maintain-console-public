package io.github.chenyilei2016.maintain.manager.pojo.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class ScriptExecutionTask {
    private String id;
    private String scriptId;
    private String scriptName;
    private String serviceName;
    private String environment;
    private ScriptTargetSelectionMode selectionMode;
    private String requestedInstanceId;
    private String executorId;
    private String executorName;
    private ScriptExecutionTaskStatus status;
    private List<ScriptExecutionTargetResult> targets;
    private Integer timeoutSeconds;
    private boolean cancelRequested;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private String approvalId;
    private boolean production;

    public boolean isTerminal() {
        return status != null && status.isTerminal();
    }
}
