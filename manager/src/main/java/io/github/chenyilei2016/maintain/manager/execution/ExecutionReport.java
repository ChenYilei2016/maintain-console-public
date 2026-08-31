package io.github.chenyilei2016.maintain.manager.execution;

import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptExecutionResult;

import java.time.LocalDateTime;
import java.util.List;

public record ExecutionReport(String id, String scriptId, int scriptVersion, String environment,
                              boolean draft, Outcome outcome, long duration, LocalDateTime startedAt,
                              List<TargetResult> targets, String warning) {
    public record TargetResult(String instanceId, String host, int port, Outcome outcome, long duration,
                               ScriptExecutionResult result, String message) {
    }

    public enum Outcome {
        SUCCESS, FAILED, UNKNOWN, NOT_STARTED, PARTIAL_SUCCESS;

        public static Outcome aggregate(List<TargetResult> targets) {
            if (targets.stream().allMatch(target -> target.outcome == SUCCESS)) return SUCCESS;
            if (targets.stream().anyMatch(target -> target.outcome == UNKNOWN)) return UNKNOWN;
            if (targets.stream().anyMatch(target -> target.outcome == SUCCESS)) return PARTIAL_SUCCESS;
            if (targets.stream().anyMatch(target -> target.outcome == FAILED)) return FAILED;
            return NOT_STARTED;
        }
    }
}
