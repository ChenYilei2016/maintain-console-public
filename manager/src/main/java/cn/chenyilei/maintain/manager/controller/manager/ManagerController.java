package cn.chenyilei.maintain.manager.controller.manager;


import cn.chenyilei.maintain.client.common.console.IMaintainConsoleExecutor;
import cn.chenyilei.maintain.client.common.dto.ApiResult;
import cn.chenyilei.maintain.client.common.dto.InvokeScriptParamSignDTO;
import cn.chenyilei.maintain.client.common.dto.InvokeScriptResultDTO;
import cn.chenyilei.maintain.manager.caller.ClientCaller;
import cn.chenyilei.maintain.manager.caller.ClientCallerContext;
import cn.chenyilei.maintain.manager.constant.SPIConstants;
import cn.chenyilei.maintain.manager.constant.ScriptPermissionEnum;
import cn.chenyilei.maintain.manager.context.LoginUserContext;
import cn.chenyilei.maintain.manager.controller.dto.ScriptEvalWebRequest;
import cn.chenyilei.maintain.manager.discovery.MaintainConsoleRegistryClientDiscovery;
import cn.chenyilei.maintain.manager.exceptions.CommonException;
import cn.chenyilei.maintain.manager.pojo.common.AjaxPageResult;
import cn.chenyilei.maintain.manager.pojo.common.AjaxResult;
import cn.chenyilei.maintain.manager.pojo.dataobject.ScriptExecutionHistoryDO;
import cn.chenyilei.maintain.manager.pojo.entity.DirectoryNode;
import cn.chenyilei.maintain.manager.pojo.entity.Script;
import cn.chenyilei.maintain.manager.pojo.entity.ScriptExecutionHistoryEntity;
import cn.chenyilei.maintain.manager.pojo.entity.ScriptPermissionEntity;
import cn.chenyilei.maintain.manager.pojo.repository.ScriptExecutionHistoryRepository;
import cn.chenyilei.maintain.manager.pojo.vo.ScriptVO;
import cn.chenyilei.maintain.manager.service.DirectoryService;
import cn.chenyilei.maintain.manager.service.ScriptContentService;
import cn.chenyilei.maintain.manager.utils.IdUtil;
import cn.chenyilei.maintain.manager.utils.ZbyProfileUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.chenyilei2016.extension.spi.ExtensionLoader;
import io.github.chenyilei2016.extension.spi.kernel.URL;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@CrossOrigin("*")
@RestController
@Slf4j
public class ManagerController implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Resource
    private IMaintainConsoleExecutor maintainConsoleExecutor;
    @Resource
    private ScriptExecutionHistoryRepository scriptExecutionHistoryRepository;

    private final MaintainConsoleRegistryClientDiscovery registryClientDiscovery = ExtensionLoader.getExtensionLoader(MaintainConsoleRegistryClientDiscovery.class).getAdaptiveExtension();

    private final ClientCaller clientCaller = ExtensionLoader.getExtensionLoader(ClientCaller.class).getAdaptiveExtension();

    @Resource
    private ScriptContentService scriptContentService;

    @Resource
    private DirectoryService directoryService;

    /**
     * 管理页
     * ctx.getBean(org.springframework.jdbc.core.JdbcTemplate.class).execute("insert into ta values (1,'2')")
     */
    @PostMapping("/manager/script/eval")
    public AjaxResult<String> scriptEval(@RequestBody @Valid ScriptEvalWebRequest scriptDTO) {
        final String employeeId = LoginUserContext.getUser().getEmployeeId();
        ScriptVO scriptVO = scriptContentService.findById(scriptDTO.getScriptId());
        //这里还用前端的脚本, 因为可能用户并没有保存
        final String frontInputScript = scriptDTO.getScript();

        if (null == scriptVO) {
            throw CommonException.createReminderException("脚本不存在或节点异常");
        }
        if (!Objects.equals(scriptDTO.getService(), scriptVO.getServiceName())) {
            throw CommonException.createReminderException("脚本不属于此服务");
        }
        if (!ScriptPermissionEntity.checkPermission(scriptVO.getDirectoryNode(), scriptVO.getScript(), employeeId, ScriptPermissionEnum.INVOKE)) {
            throw CommonException.createReminderException("没有权限进行此操作:{},{}", employeeId, "INVOKE");
        }
        String finalScriptContent = ScriptVO.mergeParamScript(frontInputScript, scriptDTO.getParams());
        log.info("接受scriptEval: {}, user:{} finalContent:{}", scriptDTO, LoginUserContext.getUser(), finalScriptContent);

        if (scriptDTO.isDebug()) {
            Object evaluate = maintainConsoleExecutor.execute(finalScriptContent);
            return AjaxResult.success(Objects.toString(evaluate), "ok");
        }

        InvokeScriptParamSignDTO invokeScriptParamDTO = new InvokeScriptParamSignDTO(finalScriptContent);
        ClientCallerContext ctx = new ClientCallerContext(scriptDTO.getService());
        ctx.setEnv(scriptDTO.getEnv());

        long startTime = System.currentTimeMillis();

        ApiResult<InvokeScriptResultDTO> apiResult = null;
        Throwable tx;
        try {
            if (ZbyProfileUtils.isLocal(applicationContext.getEnvironment())) {
                ctx.addParameter(SPIConstants.REGISTRY_CLIENT_DISCOVERY_TYPE, "local");
            }
            apiResult = clientCaller.$invokeScript(ctx, invokeScriptParamDTO);
        } catch (Throwable e) {
            //todo: 异常处理 , 记录
            tx = e;
        } finally {
            saveExecutionHistory(frontInputScript, scriptDTO.getParams(), finalScriptContent, scriptVO, apiResult, startTime, System.currentTimeMillis());
        }
        if (apiResult.isSuccess()) {
            return AjaxResult.success(apiResult.getData().getScriptResult(), apiResult.getMsg());
        } else {
            return AjaxResult.success(apiResult.getMsg(), apiResult.getMsg());
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
            historyEntity.setExecutorId(LoginUserContext.getUser().getEmployeeId());
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
                } else {
                    historyEntity.setStatus("error");
                    historyEntity.setResult(apiResult.getMsg());
                }
            } else {
                historyEntity.setStatus("error");
                historyEntity.setErrorMessage("Result is null");
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
        return new AjaxPageResult(true, historyPage.getRecords(), null, page, size, historyPage.getTotal());
    }

    @PostMapping("/manager/service/list")
    public AjaxResult<List<String>> serviceList() {
        URL url = new URL();
        if (ZbyProfileUtils.isLocal(applicationContext.getEnvironment())) {
            url.addParameter(SPIConstants.REGISTRY_CLIENT_DISCOVERY_TYPE, "local");
        }
        return AjaxResult.success(registryClientDiscovery.listServiceNames(url));
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
