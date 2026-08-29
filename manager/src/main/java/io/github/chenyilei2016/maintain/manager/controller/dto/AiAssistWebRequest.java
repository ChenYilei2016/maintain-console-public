package io.github.chenyilei2016.maintain.manager.controller.dto;

import io.github.chenyilei2016.maintain.manager.pojo.entity.AiAssistAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiAssistWebRequest(
        @NotNull AiAssistAction action,
        @Size(max = 64) String scriptId,
        @Size(max = 255) String serviceName,
        @Size(max = 100_000) String script,
        @Size(max = 50_000) String parameterSchema,
        @Size(max = 10_000) String instruction
) {
}
