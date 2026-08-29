package io.github.chenyilei2016.maintain.manager.controller.dto.res;

import io.github.chenyilei2016.maintain.manager.pojo.entity.AiAssistAction;

public record AiAssistWebResponse(
        AiAssistAction action,
        String content,
        String model,
        String notice
) {
}
