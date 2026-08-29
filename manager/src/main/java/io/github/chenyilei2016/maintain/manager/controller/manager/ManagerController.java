package io.github.chenyilei2016.maintain.manager.controller.manager;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.chenyilei2016.maintain.client.common.console.IMaintainConsoleExecutor;
import io.github.chenyilei2016.maintain.client.common.dto.ApiResult;
import io.github.chenyilei2016.maintain.client.common.dto.InvokeScriptResultDTO;
import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import io.github.chenyilei2016.maintain.manager.constant.ScriptPermissionEnum;
import io.github.chenyilei2016.maintain.manager.context.LoginUserContext;
import io.github.chenyilei2016.maintain.manager.controller.dto.DevopsScriptEvalWebRequest;
import io.github.chenyilei2016.maintain.manager.controller.dto.ScriptEvalPreviewWebRequest;
import io.github.chenyilei2016.maintain.manager.controller.dto.ScriptEvalWebRequest;
import io.github.chenyilei2016.maintain.manager.discovery.MaintainConsoleRegistryClientDiscovery;
import io.github.chenyilei2016.maintain.manager.exceptions.CommonException;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxPageResult;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxResult;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ScriptExecutionHistoryDO;
import io.github.chenyilei2016.maintain.manager.pojo.entity.*;
import io.github.chenyilei2016.maintain.manager.pojo.repository.ScriptExecutionHistoryRepository;
import io.github.chenyilei2016.maintain.manager.pojo.vo.ScriptVO;
import io.github.chenyilei2016.maintain.manager.service.EnvironmentCatalogService;
import io.github.chenyilei2016.maintain.manager.service.ScriptContentService;
import io.github.chenyilei2016.maintain.manager.service.ScriptInvoker;
import io.github.chenyilei2016.maintain.manager.utils.IdUtil;
import io.github.chenyilei2016.maintain.manager.utils.MyProfileUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@RestController
@Slf4j
public class ManagerController {
    private final ScriptExecutionHistoryRepository scriptExecutionHistoryRepository;
    private final MaintainConsoleRegistryClientDiscovery registryClientDiscovery;
    private final ScriptInvoker scriptInvoker;
    private final ScriptContentService scriptContentService;
    private final ManagerProperties managerProperties;
    private final EnvironmentCatalogService environmentCatalogService;
    private final Environment environment;

    public ManagerController(
            ScriptExecutionHistoryRepository scriptExecutionHistoryRepository,
            MaintainConsoleRegistryClientDiscovery registryClientDiscovery,
            ScriptInvoker scriptInvoker,
            ScriptContentService scriptContentService,
            ManagerProperties managerProperties,
            EnvironmentCatalogService environmentCatalogService,
            Environment environment
    ) {
        this.scriptExecutionHistoryRepository = scriptExecutionHistoryRepository;
        this.registryClientDiscovery = registryClientDiscovery;
        this.scriptInvoker = scriptInvoker;
        this.scriptContentService = scriptContentService;
        this.managerProperties = managerProperties;
        this.environmentCatalogService = environmentCatalogService;
        this.environment = environment;
    }


    /**
     * 外部接口调用, 直接使用ID
     */
    @PostMapping("/devops/manager/script/eval")
    public AjaxResult<String> scriptEvalRpc(@RequestBody @Valid DevopsScriptEvalWebRequest scriptDTO) {
        rejectUnsafeLegacyProductionExecution(scriptDTO.getEnv());
        final String employeeId = LoginUserContext.getUser().getEmployeeNo();
        ScriptVO scriptVO = scriptContentService.findById(scriptDTO.getScriptId());

        if (null == scriptVO) {
            throw CommonException.createReminderException("脚本不存在或节点异常");
        }
        if (!Objects.equals(scriptDTO.getService(), scriptVO.getServiceName())) {
            throw CommonException.createReminderException("脚本不属于此服务");
        }
        if (!ScriptPermissionEntity.checkPermission(scriptVO.getDirectoryNode(), scriptVO.getScript(), employeeId,
                ScriptPermissionEnum.INVOKE, managerProperties.getGlobalWhiteEmployeeNoList())) {
            throw CommonException.createReminderException("没有权限进行此操作:{},{}", employeeId, "INVOKE");
        }

        ScriptParameterSchema.ResolvedScript resolvedScript = ScriptVO.resolveParamScript(
                scriptVO.getScriptContent(), scriptDTO.getParams(), scriptVO.getScript().getParameterSchema());
        String finalScriptContent = resolvedScript.executableContent();
        log.info("接收脚本执行请求, service:{}, scriptId:{}, user:{}",
                scriptDTO.getService(), scriptDTO.getScriptId(), LoginUserContext.getUser().getEmployeeNo());

        long startTime = System.currentTimeMillis();

        ApiResult<InvokeScriptResultDTO> apiResult = null;
        try {
            apiResult = scriptInvoker.invoke(scriptDTO.getService(), scriptDTO.getEnv(), null,
                    finalScriptContent, resolvedScript);
        } catch (RuntimeException e) {
            log.warn("脚本执行失败, service:{}, scriptId:{}", scriptDTO.getService(), scriptDTO.getScriptId(), e);
            apiResult = ApiResult.error(e.getMessage());
        } finally {
            saveExecutionHistory(scriptVO.getScriptContent(), resolvedScript.persistedParameters(), resolvedScript.persistedContent(),
                    scriptVO, apiResult, startTime, System.currentTimeMillis());
        }
        if (apiResult != null && apiResult.isSuccess()) {
            return AjaxResult.success(apiResult.getData().getScriptResult(), apiResult.getMsg());
        }
        return AjaxResult.error(apiResult == null ? "脚本执行失败" : apiResult.getMsg());
    }

    @PostMapping("/manager/script/preview")
    public AjaxResult<String> preview(@RequestBody @Valid ScriptEvalPreviewWebRequest scriptDTO) {
        final String frontInputScript = scriptDTO.getScript();
        return AjaxResult.success(ScriptVO.resolveParamScript(
                frontInputScript, scriptDTO.getParams(), scriptDTO.getParameterSchema()).executableContent());
    }

    /**
     * 管理页
     * ctx.getBean(org.springframework.jdbc.core.JdbcTemplate.class).execute("insert into ta values (1,'2')")
     */
    @PostMapping("/manager/script/eval")
    public AjaxResult<String> scriptEval(@RequestBody @Valid ScriptEvalWebRequest scriptDTO) {
        rejectUnsafeLegacyProductionExecution(scriptDTO.getEnv());
        final String employeeNo = LoginUserContext.getUser().getEmployeeNo();
        ScriptVO scriptVO = scriptContentService.findById(scriptDTO.getScriptId());
        //这里还用前端的脚本, 因为可能用户并没有保存
        final String frontInputScript = scriptDTO.getScript();

        if (null == scriptVO) {
            throw CommonException.createReminderException("脚本不存在或节点异常");
        }
        if (!Objects.equals(scriptDTO.getService(), scriptVO.getServiceName())) {
            throw CommonException.createReminderException("脚本不属于此服务");
        }
        if (!ScriptPermissionEntity.checkPermission(scriptVO.getDirectoryNode(), scriptVO.getScript(), employeeNo,
                ScriptPermissionEnum.INVOKE, managerProperties.getGlobalWhiteEmployeeNoList())) {
            throw CommonException.createReminderException("没有权限进行此操作:{},{}", employeeNo, "INVOKE");
        }
        String parameterSchema = scriptDTO.getParameterSchema() == null
                ? scriptVO.getScript().getParameterSchema()
                : scriptDTO.getParameterSchema();
        ScriptParameterSchema.ResolvedScript resolvedScript = ScriptVO.resolveParamScript(
                frontInputScript, scriptDTO.getParams(), parameterSchema);
        String finalScriptContent = resolvedScript.executableContent();
        log.info("接收脚本执行请求, service:{}, scriptId:{}, user:{}",
                scriptDTO.getService(), scriptDTO.getScriptId(), LoginUserContext.getUser().getEmployeeNo());

        if (scriptDTO.isDebug()) {
            if (!MyProfileUtils.isLocal(environment)) {
                throw CommonException.createReminderException("仅本地环境允许调试执行");
            }
            Object evaluate = getLocalExecutor().execute(finalScriptContent);
            return AjaxResult.success(Objects.toString(evaluate), "ok");
        }

        long startTime = System.currentTimeMillis();

        ApiResult<InvokeScriptResultDTO> apiResult = null;
        try {
            apiResult = scriptInvoker.invoke(scriptDTO.getService(), scriptDTO.getEnv(), null,
                    finalScriptContent, resolvedScript);
        } catch (RuntimeException e) {
            log.warn("脚本执行失败, service:{}, scriptId:{}", scriptDTO.getService(), scriptDTO.getScriptId(), e);
            apiResult = ApiResult.error(e.getMessage());
        } finally {
            saveExecutionHistory(frontInputScript, resolvedScript.persistedParameters(), resolvedScript.persistedContent(),
                    scriptVO, apiResult, startTime, System.currentTimeMillis());
        }
        if (apiResult != null && apiResult.isSuccess()) {
            return AjaxResult.success(apiResult.getData().getScriptResult(), apiResult.getMsg());
        }
        return AjaxResult.error(apiResult == null ? "脚本执行失败" : apiResult.getMsg());
    }

    @PostMapping("/manager/script/eval/v2")
    public AjaxResult<ScriptExecutionResult> scriptEvalV2(@RequestBody @Valid ScriptEvalWebRequest request) {
        AjaxResult<String> legacyResult = scriptEval(request);
        if (!legacyResult.isSuccess()) {
            return AjaxResult.error(legacyResult.getMsg());
        }
        return AjaxResult.success(ScriptExecutionResult.fromRaw(legacyResult.getData()), legacyResult.getMsg());
    }

    private IMaintainConsoleExecutor getLocalExecutor() {
        return scriptInvoker.getLocalExecutor();
    }

    private void rejectUnsafeLegacyProductionExecution(String targetEnvironment) {
        if (environmentCatalogService.isProduction(targetEnvironment)
                && !managerProperties.getSecurity().isAllowLegacySynchronousExecution()) {
            throw CommonException.createReminderException("生产环境已禁用无审批的同步执行接口，请使用执行任务流程");
        }
    }

    private void saveExecutionHistory(String frontInputScript, String params, String finalScriptContent, ScriptVO vo, ApiResult<InvokeScriptResultDTO> apiResult, long startTime, long endTime) {
        try {
            DirectoryNode node = vo.getDirectoryNode();
            Script script = vo.getScript();

            ScriptExecutionHistoryEntity historyEntity = new ScriptExecutionHistoryEntity();
            historyEntity.setId(IdUtil.generateSnowFlakeId());
            historyEntity.setScriptName(node.getName()); // Or from scriptDTO if available
            historyEntity.setServiceName(node.getServiceName());
            historyEntity.setExecutorId(LoginUserContext.getUser().getEmployeeNo());
            historyEntity.setExecutorName(LoginUserContext.getUser().getEmployeeName());
            historyEntity.setScriptId(script.getId());
            historyEntity.setScriptContent(frontInputScript);
            historyEntity.setFinalScriptContent(finalScriptContent);
            historyEntity.setParameters(params);
            historyEntity.setStartTime(new java.sql.Timestamp(startTime).toLocalDateTime());
            historyEntity.setEndTime(new java.sql.Timestamp(endTime).toLocalDateTime());
            historyEntity.setDuration((int) (endTime - startTime));
            historyEntity.setCreateTime(LocalDateTime.now());

            if (apiResult != null) {
                if (apiResult.isSuccess()) {
                    historyEntity.setStatus("success");
                    historyEntity.setResult(apiResult.getData().getScriptResult());
                    ScriptExecutionResult resultPayload = ScriptExecutionResult.fromRaw(apiResult.getData().getScriptResult());
                    historyEntity.setProtocolVersion(resultPayload.getProtocolVersion());
                    historyEntity.setResultPayload(resultPayload.toJson());
                } else {
                    historyEntity.setStatus("error");
                    historyEntity.setResult(apiResult.getMsg());
                    ScriptExecutionResult resultPayload = ScriptExecutionResult.error(apiResult.getMsg());
                    historyEntity.setProtocolVersion(resultPayload.getProtocolVersion());
                    historyEntity.setResultPayload(resultPayload.toJson());
                }
            } else {
                historyEntity.setStatus("error");
                historyEntity.setErrorMessage("Result is null");
                ScriptExecutionResult resultPayload = ScriptExecutionResult.error("Result is null");
                historyEntity.setProtocolVersion(resultPayload.getProtocolVersion());
                historyEntity.setResultPayload(resultPayload.toJson());
            }
            scriptExecutionHistoryRepository.save(historyEntity);
        } catch (Exception e) {
            log.error("保存脚本执行历史失败", e);
        }
    }

    @GetMapping("/manager/script/history")
    public AjaxPageResult<List<ScriptExecutionHistoryEntity>> scriptHistory(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam String scriptId) {
        Page<ScriptExecutionHistoryEntity> pageRequest = new Page<>(page, size);
        QueryWrapper<ScriptExecutionHistoryDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("script_id", scriptId);
        queryWrapper.orderByDesc("id");
        IPage<ScriptExecutionHistoryEntity> historyPage = scriptExecutionHistoryRepository.page(pageRequest, queryWrapper);
        return new AjaxPageResult<>(true, historyPage.getRecords(), null, page, size, historyPage.getTotal());
    }

    @PostMapping("/manager/service/list")
    public AjaxResult<List<String>> serviceList() {
        return AjaxResult.success(registryClientDiscovery.listServiceNames());
    }

}
