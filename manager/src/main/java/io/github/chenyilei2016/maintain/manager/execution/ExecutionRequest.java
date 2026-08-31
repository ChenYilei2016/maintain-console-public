package io.github.chenyilei2016.maintain.manager.execution;

import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptTargetSelectionMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.Map;

/**
 * 两个独立的输入契约，运行者没有提交源码、Schema 或服务名的字段。
 */
public final class ExecutionRequest {
    private ExecutionRequest() {
    }

    public record Target(@NotBlank String environment, @NotNull ScriptTargetSelectionMode selectionMode,
                         String instanceId, @Min(1) @Max(900) int timeoutSeconds) {
    }

    public record RunSaved(@NotBlank String scriptId, @NotNull @Min(1) Integer version,
                           @NotNull @Size(max = 100) Map<String, Object> parameters,
                           @NotNull @Valid Target target, boolean riskConfirmed) {
    }

    public record DebugDraft(@NotBlank String scriptId, @NotNull @Min(1) Integer version,
                             @NotBlank @Size(max = 1_048_576) String content,
                             @Size(max = 262_144) String parameterSchema,
                             @NotNull @Size(max = 100) Map<String, Object> parameters,
                             @NotNull @Valid Target target, boolean riskConfirmed) {
    }
}
