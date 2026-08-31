package io.github.chenyilei2016.maintain.manager.execution;

import com.alibaba.fastjson2.JSON;
import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import io.github.chenyilei2016.maintain.manager.constant.ScriptPermissionEnum;
import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;
import io.github.chenyilei2016.maintain.manager.discovery.MaintainConsoleRegistryClientDiscovery;
import io.github.chenyilei2016.maintain.manager.exceptions.CommonException;
import io.github.chenyilei2016.maintain.manager.pojo.entity.*;
import io.github.chenyilei2016.maintain.manager.pojo.repository.ScriptExecutionHistoryRepository;
import io.github.chenyilei2016.maintain.manager.pojo.vo.ScriptVO;
import io.github.chenyilei2016.maintain.manager.service.AuditLogService;
import io.github.chenyilei2016.maintain.manager.service.EnvironmentCatalogService;
import io.github.chenyilei2016.maintain.manager.service.ScriptAccessControl;
import io.github.chenyilei2016.maintain.manager.service.ScriptInvoker;
import io.github.chenyilei2016.maintain.manager.utils.IdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.chenyilei2016.maintain.manager.execution.ExecutionReport.Outcome.*;

/**
 * 当前请求内完成一次执行；没有任务持久化、恢复、订阅或自动重试。
 */
@Slf4j
@Service
public class ScriptExecutionService {
    private static final int MAX_PARAMETERS_BYTES = 262_144;
    private static final int MAX_REPORT_BYTES = 2 * 1024 * 1024;
    private final ScriptAccessControl access;
    private final EnvironmentCatalogService environments;
    private final MaintainConsoleRegistryClientDiscovery discovery;
    private final ScriptInvoker invoker;
    private final ScriptExecutionHistoryRepository histories;
    private final AuditLogService audit;
    private final ManagerProperties properties;
    private final ThreadPoolTaskExecutor executor;

    public ScriptExecutionService(ScriptAccessControl access, EnvironmentCatalogService environments,
                                  MaintainConsoleRegistryClientDiscovery discovery, ScriptInvoker invoker,
                                  ScriptExecutionHistoryRepository histories, AuditLogService audit,
                                  ManagerProperties properties,
                                  @Qualifier("executionTargetExecutor") ThreadPoolTaskExecutor executor) {
        this.access = access;
        this.environments = environments;
        this.discovery = discovery;
        this.invoker = invoker;
        this.histories = histories;
        this.audit = audit;
        this.properties = properties;
        this.executor = executor;
    }

    public ExecutionReport runSaved(ExecutionRequest.RunSaved request, LocalLoginUser actor) {
        long started = System.nanoTime();
        ScriptVO tool = access.require(request.scriptId(), actor.getEmployeeNo(), ScriptPermissionEnum.INVOKE);
        requireVersion(tool, request.version());
        if (tool.getScript().getParameterSchema() == null || tool.getScript().getParameterSchema().isBlank()) {
            throw CommonException.createReminderException("此工具尚未配置类型化参数，请作者在工作台确认迁移后再分享运行");
        }
        return execute(tool, tool.getScriptContent(), tool.getScript().getParameterSchema(), request.parameters(),
                request.target(), request.riskConfirmed(), false, actor, started);
    }

    public ExecutionReport debugDraft(ExecutionRequest.DebugDraft request, LocalLoginUser actor) {
        long started = System.nanoTime();
        ScriptVO tool = access.require(request.scriptId(), actor.getEmployeeNo(), ScriptPermissionEnum.EDIT);
        if (!access.allows(tool, actor.getEmployeeNo(), ScriptPermissionEnum.INVOKE)) {
            throw CommonException.createReminderException("草稿调试同时需要编辑和执行权限");
        }
        requireVersion(tool, request.version());
        return execute(tool, request.content(), request.parameterSchema(), request.parameters(), request.target(),
                request.riskConfirmed(), true, actor, started);
    }

    private void requireVersion(ScriptVO tool, Integer version) {
        if (!Objects.equals(version, tool.getScript().getVersion())) {
            throw CommonException.createReminderException("工具版本已变化或未提供版本，请刷新并核对后运行");
        }
    }

    private ExecutionReport execute(ScriptVO tool, String content, String schema, Map<String, Object> values,
                                    ExecutionRequest.Target target, boolean riskConfirmed, boolean draft,
                                    LocalLoginUser actor, long started) {
        LocalDateTime startedAt = LocalDateTime.now();
        ScriptPermissionEntity grants = ScriptPermissionEntity.parse(tool.getScriptPermissions());
        if (!grants.isEnabled()) throw CommonException.createReminderException("工具已停用");
        ManagerProperties.TargetEnvironment environment = environments.require(target.environment());
        if (!grants.allowsEnvironment(environment.getValue(), draft)) {
            throw CommonException.createReminderException("工具未授权此环境，请联系创建者配置允许环境");
        }
        if (target.selectionMode() == ScriptTargetSelectionMode.ALL && !grants.isAllowAllInstances()) {
            throw CommonException.createReminderException("此工具未允许全部实例执行");
        }
        if (target.timeoutSeconds() < 1 || target.timeoutSeconds() > properties.getExecution().getMaxTimeoutSeconds()) {
            throw CommonException.createReminderException("等待时间超出配置上限");
        }
        ScriptToolMetadata metadata = ScriptToolMetadata.parse(tool.getScript().getToolMetadata());
        if (!riskConfirmed && (environment.isProduction() || metadata.getOperationType().requiresConfirmation())) {
            throw CommonException.createReminderException("请确认目标与操作风险；确认不是审批或安全隔离");
        }
        String params = JSON.toJSONString(values);
        if (params.getBytes(StandardCharsets.UTF_8).length > MAX_PARAMETERS_BYTES) {
            throw CommonException.createReminderException("参数超过大小上限");
        }
        ScriptParameterSchema.ResolvedScript resolved = ScriptVO.resolveParamScript(content, params, schema);
        List<ServiceInstance> instances = target.selectionMode().select(
                discovery.listServiceInstances(tool.getServiceName(), environment.getValue()),
                target.instanceId(), properties.getExecution().getMaxTargets());
        long deadline = started + TimeUnit.SECONDS.toNanos(target.timeoutSeconds());
        if (System.nanoTime() >= deadline)
            throw CommonException.createReminderException("准备执行已超时，本次未发起调用");
        // 身份来自可信上下文，作为签名脚本的一部分传给 Client，不使用表单中的身份字段。
        String caller = "def _caller = [employeeNo: " + ScriptParameterSchema.groovyStringLiteral(actor.getEmployeeNo())
                + ", employeeName: " + ScriptParameterSchema.groovyStringLiteral(actor.getEmployeeName()) + "]\n";
        String executable = caller + resolved.executableContent();
        List<PendingCall> calls = new ArrayList<>();
        for (ServiceInstance instance : instances) {
            AtomicBoolean sent = new AtomicBoolean();
            Future<ExecutionReport.TargetResult> future = null;
            try {
                future = executor.submit(() -> {
                    long callStart = System.nanoTime();
                    if (callStart >= deadline)
                        return targetResult(instance, NOT_STARTED, 0, null, "等待时间已到，未发起调用");
                    sent.set(true);
                    try {
                        var response = invoker.invoke(tool.getServiceName(), environment.getValue(), instance,
                                executable, resolved, Math.max(1, TimeUnit.NANOSECONDS.toMillis(deadline - callStart)));
                        if (response == null)
                            return targetResult(instance, UNKNOWN, elapsed(callStart), null, "客户端未返回结果，操作可能已经发生");
                        if (!response.isSuccess()) return targetResult(instance, FAILED, elapsed(callStart), null,
                                draft ? ScriptExecutionResult.fromRaw(resolved.sanitizeResult(response.getMsg())).primaryText()
                                        : "客户端报告执行失败；不代表已撤销业务操作，请联系工具作者核查");
                        return targetResult(instance, SUCCESS, elapsed(callStart),
                                ScriptExecutionResult.fromRaw(response.getData().getScriptResult()), null);
                    } catch (RuntimeException exception) {
                        // 不记录异常正文：Groovy/数据库异常可能携带源码或业务敏感值。
                        log.warn("调用结果未知, scriptId:{}, instanceId:{}, exception:{}", tool.getScript().getId(),
                                ServiceInstanceDTO.idOf(instance), exception.getClass().getSimpleName());
                        return targetResult(instance, UNKNOWN, elapsed(callStart), null, "调用未获得最终结果，远端操作可能仍在继续；请勿直接重复执行");
                    }
                });
            } catch (RejectedExecutionException rejected) {
                // 已发出的其他目标照常汇总；不能把部分执行描述成整体未执行。
            }
            calls.add(new PendingCall(instance, sent, future));
        }
        List<ExecutionReport.TargetResult> results = new ArrayList<>();
        for (PendingCall call : calls) {
            if (call.future == null) {
                results.add(targetResult(call.instance, NOT_STARTED, 0, null, "执行容量已满，此实例未开始"));
                continue;
            }
            try {
                results.add(call.future.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                call.future.cancel(true);
                results.add(targetResult(call.instance, call.sent.get() ? UNKNOWN : NOT_STARTED,
                        elapsed(started), null, "请求已停止等待，不代表远端操作已终止"));
            } catch (ExecutionException | TimeoutException | CancellationException unavailable) {
                call.future.cancel(true);
                results.add(targetResult(call.instance, call.sent.get() ? UNKNOWN : NOT_STARTED,
                        elapsed(started), null, "等待超时或连接中断；已发出的操作结果未知，不自动重试"));
            }
        }
        int resultBytes = 0;
        for (int index = 0; index < results.size(); index++) {
            ExecutionReport.TargetResult item = results.get(index);
            if (item.result() == null) continue;
            resultBytes += item.result().toJson().getBytes(StandardCharsets.UTF_8).length;
            if (resultBytes > MAX_REPORT_BYTES) results.set(index, new ExecutionReport.TargetResult(
                    item.instanceId(), item.host(), item.port(), item.outcome(), item.duration(), null,
                    "当前返回结果超过总大小上限，已截断；不代表完整查询数据"));
        }
        var outcome = ExecutionReport.Outcome.aggregate(results);
        String id = IdUtil.generateSnowFlakeId();
        String warning = null;
        ScriptExecutionResult payload = results.size() == 1 && results.getFirst().result() != null
                ? results.getFirst().result() : ScriptExecutionResult.fromRaw(JSON.toJSONString(results));
        try {
            boolean saved = histories.save(new ScriptExecutionHistoryEntity().setId(id).setScriptId(tool.getScript().getId())
                    .setScriptName(tool.getDirectoryNode().getName()).setServiceName(tool.getServiceName())
                    .setExecutorId(actor.getEmployeeNo()).setExecutorName(actor.getEmployeeName())
                    .setScriptContent("").setFinalScriptContent("").setParameters(resolved.persistedParameters())
                    .setScriptVersion(tool.getScript().getVersion()).setEnvironment(environment.getValue()).setDraft(draft)
                    .setTargetsJson(JSON.toJSONString(results)).setOutcome(outcome.name())
                    .setStatus(outcome == SUCCESS ? "success" : "error")
                    .setResult(payload.primaryText()).setResultPayload(payload.toJson()).setProtocolVersion(1)
                    .setStartTime(startedAt).setEndTime(LocalDateTime.now()).setDuration(Math.toIntExact(elapsed(started)))
                    .setCreateTime(LocalDateTime.now()));
            if (!saved) throw new IllegalStateException("执行记录未写入");
        } catch (RuntimeException historyFailure) {
            log.error("执行记录写入失败, executionId:{}, exception:{}", id, historyFailure.getClass().getSimpleName());
            warning = "业务调用已结束，但执行记录未能保存；请保留本次结果，不要因此重复执行";
        }
        audit.record(actor, draft ? "SCRIPT_DRAFT_DEBUG" : "TOOL_RUN", "SCRIPT", tool.getScript().getId(),
                outcome.name(), Map.of("executionId", id, "version", tool.getScript().getVersion(),
                        "environment", environment.getValue(), "targetCount", results.size()));
        return new ExecutionReport(id, tool.getScript().getId(), tool.getScript().getVersion(), environment.getValue(),
                draft, outcome, elapsed(started), startedAt, List.copyOf(results), warning);
    }

    private static ExecutionReport.TargetResult targetResult(ServiceInstance instance, ExecutionReport.Outcome outcome,
                                                             long duration, ScriptExecutionResult result, String message) {
        return new ExecutionReport.TargetResult(ServiceInstanceDTO.idOf(instance), instance.getHost(), instance.getPort(),
                outcome, duration, result, message);
    }

    private static long elapsed(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    private record PendingCall(ServiceInstance instance, AtomicBoolean sent,
                               Future<ExecutionReport.TargetResult> future) {
    }
}
