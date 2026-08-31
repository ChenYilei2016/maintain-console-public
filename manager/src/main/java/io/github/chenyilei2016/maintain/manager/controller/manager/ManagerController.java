package io.github.chenyilei2016.maintain.manager.controller.manager;

import com.alibaba.fastjson2.JSON;
import io.github.chenyilei2016.maintain.manager.constant.ScriptPermissionEnum;
import io.github.chenyilei2016.maintain.manager.context.LoginUserContext;
import io.github.chenyilei2016.maintain.manager.controller.dto.DevopsScriptEvalWebRequest;
import io.github.chenyilei2016.maintain.manager.controller.dto.ScriptEvalPreviewWebRequest;
import io.github.chenyilei2016.maintain.manager.controller.dto.ScriptEvalWebRequest;
import io.github.chenyilei2016.maintain.manager.discovery.MaintainConsoleRegistryClientDiscovery;
import io.github.chenyilei2016.maintain.manager.execution.ExecutionReport;
import io.github.chenyilei2016.maintain.manager.execution.ExecutionRequest;
import io.github.chenyilei2016.maintain.manager.execution.ScriptExecutionService;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxResult;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptExecutionResult;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptTargetSelectionMode;
import io.github.chenyilei2016.maintain.manager.pojo.vo.ScriptVO;
import io.github.chenyilei2016.maintain.manager.service.ScriptAccessControl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ManagerController {
    private final ScriptExecutionService executions;
    private final ScriptAccessControl access;
    private final MaintainConsoleRegistryClientDiscovery discovery;

    @PostMapping("/manager/tools/run")
    public AjaxResult<ExecutionReport> runSaved(@RequestBody @Valid ExecutionRequest.RunSaved request) {
        return AjaxResult.success(executions.runSaved(request, LoginUserContext.getUser()));
    }

    @PostMapping("/manager/scripts/debug")
    public AjaxResult<ExecutionReport> debugDraft(@RequestBody @Valid ExecutionRequest.DebugDraft request) {
        return AjaxResult.success(executions.debugDraft(request, LoginUserContext.getUser()));
    }

    /**
     * 旧外部入口要求升级携带已看版本；不能由服务端代填最新版本。
     */
    @PostMapping("/devops/manager/script/eval")
    public AjaxResult<String> legacySaved(@RequestBody @Valid DevopsScriptEvalWebRequest request) {
        ExecutionReport report = executions.runSaved(new ExecutionRequest.RunSaved(request.getScriptId(),
                request.getVersion(), parameters(request.getParams()),
                new ExecutionRequest.Target(request.getEnv(), ScriptTargetSelectionMode.RANDOM, null, 180),
                request.isRiskConfirmed()), LoginUserContext.getUser());
        return legacyResult(report);
    }

    /** 旧编辑入口同样进入草稿调试，不再存在 local 直连绕过路径。 */
    @PostMapping("/manager/script/eval")
    public AjaxResult<String> legacyDebug(@RequestBody @Valid ScriptEvalWebRequest request) {
        return legacyResult(debugLegacy(request));
    }

    @PostMapping("/manager/script/eval/v2")
    public AjaxResult<ScriptExecutionResult> legacyDebugV2(@RequestBody @Valid ScriptEvalWebRequest request) {
        ExecutionReport report = debugLegacy(request);
        if (report.outcome() != ExecutionReport.Outcome.SUCCESS)
            return AjaxResult.error(report.targets().getFirst().message());
        return AjaxResult.success(report.targets().getFirst().result());
    }

    @PostMapping("/manager/script/preview")
    public AjaxResult<String> preview(@RequestBody @Valid ScriptEvalPreviewWebRequest request) {
        access.require(request.getScriptId(), LoginUserContext.getUser().getEmployeeNo(), ScriptPermissionEnum.EDIT);
        return AjaxResult.success(ScriptVO.resolveParamScript(
                request.getScript(), request.getParams(), request.getParameterSchema()).executableContent());
    }

    @PostMapping("/manager/service/list")
    public AjaxResult<List<String>> serviceList() {
        return AjaxResult.success(discovery.listServiceNames());
    }

    private ExecutionReport debugLegacy(ScriptEvalWebRequest request) {
        return executions.debugDraft(new ExecutionRequest.DebugDraft(request.getScriptId(), request.getVersion(),
                request.getScript(), request.getParameterSchema(), parameters(request.getParams()),
                new ExecutionRequest.Target(request.getEnv(), ScriptTargetSelectionMode.RANDOM, null, 180),
                request.isRiskConfirmed()), LoginUserContext.getUser());
    }

    private AjaxResult<String> legacyResult(ExecutionReport report) {
        if (report.outcome() != ExecutionReport.Outcome.SUCCESS)
            return AjaxResult.error(report.targets().getFirst().message());
        return AjaxResult.success(report.targets().getFirst().result().primaryText(), report.warning());
    }

    private Map<String, Object> parameters(String json) {
        Map<String, Object> values = json == null ? Map.of() : JSON.parseObject(json);
        if (values == null) throw new IllegalArgumentException("参数必须为 JSON 对象");
        return values;
    }
}
