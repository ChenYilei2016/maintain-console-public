package io.github.chenyilei2016.maintain.manager.controller.manager;

import io.github.chenyilei2016.maintain.manager.context.LoginUserContext;
import io.github.chenyilei2016.maintain.manager.controller.dto.AiAssistWebRequest;
import io.github.chenyilei2016.maintain.manager.controller.dto.res.AiAssistWebResponse;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxResult;
import io.github.chenyilei2016.maintain.manager.service.AiAssistantService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/ai")
public class AiAssistantController {
    private final AiAssistantService aiAssistantService;

    public AiAssistantController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/assist")
    public AjaxResult<AiAssistWebResponse> assist(@Valid @RequestBody AiAssistWebRequest request) {
        return AjaxResult.success(aiAssistantService.assist(request, LoginUserContext.getUser()));
    }
}
