package io.github.chenyilei2016.maintain.manager.pojo.entity;

import java.util.List;

public enum ScriptExecutionTaskStatus {
    QUEUED,
    RUNNING,
    SUCCESS,
    FAILED,
    PARTIAL_SUCCESS,
    CANCELLING,
    CANCELLED,
    TIMED_OUT;

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == PARTIAL_SUCCESS || this == CANCELLED || this == TIMED_OUT;
    }

    public static ScriptExecutionTaskStatus aggregate(List<ScriptExecutionTargetResult> targets) {
        long successes = targets.stream().filter(target -> target.getStatus() == SUCCESS).count();
        if (successes == targets.size()) {
            return SUCCESS;
        }
        return successes == 0 ? FAILED : PARTIAL_SUCCESS;
    }
}
