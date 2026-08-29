package io.github.chenyilei2016.maintain.manager.pojo.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ScriptExecutionTargetResult {
    private ServiceInstanceDTO instance;
    private ScriptExecutionTaskStatus status;
    private Integer duration;
    private ScriptExecutionResult result;
    private String errorMessage;
}
