package io.github.chenyilei2016.maintain.manager.controller.manager;

import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;
import io.github.chenyilei2016.maintain.manager.context.LoginUserContext;
import io.github.chenyilei2016.maintain.manager.controller.dto.ScriptFavoriteWebRequest;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxResult;
import io.github.chenyilei2016.maintain.manager.pojo.dto.ScriptResourceOverviewDTO;
import io.github.chenyilei2016.maintain.manager.service.AuditLogService;
import io.github.chenyilei2016.maintain.manager.service.ScriptUserPreferenceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
@RequestMapping("/manager/resources")
public class ScriptUserPreferenceController {
    private final ScriptUserPreferenceService preferenceService;
    private final AuditLogService auditLogService;

    public ScriptUserPreferenceController(
            ScriptUserPreferenceService preferenceService,
            AuditLogService auditLogService
    ) {
        this.preferenceService = preferenceService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/overview")
    public AjaxResult<ScriptResourceOverviewDTO> overview(@RequestParam @NotBlank String serviceName) {
        return AjaxResult.success(preferenceService.overview(LoginUserContext.getUser().getEmployeeNo(), serviceName));
    }

    @PostMapping("/favorite")
    public AjaxResult<Boolean> favorite(@RequestBody @Valid ScriptFavoriteWebRequest request) {
        LocalLoginUser user = LoginUserContext.getUser();
        preferenceService.favorite(user.getEmployeeNo(), request.getScriptId(), request.isFavorite());
        auditLogService.record(user, request.isFavorite() ? "SCRIPT_FAVORITE" : "SCRIPT_UNFAVORITE",
                "SCRIPT", request.getScriptId(), "SUCCESS", Map.of());
        return AjaxResult.success(request.isFavorite());
    }
}
