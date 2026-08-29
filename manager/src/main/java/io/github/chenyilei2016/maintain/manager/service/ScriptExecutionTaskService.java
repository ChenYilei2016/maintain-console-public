package io.github.chenyilei2016.maintain.manager.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.chenyilei2016.maintain.client.common.dto.ApiResult;
import io.github.chenyilei2016.maintain.client.common.dto.InvokeScriptResultDTO;
import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;
import io.github.chenyilei2016.maintain.manager.controller.dto.ExecutionTaskCreateWebRequest;
import io.github.chenyilei2016.maintain.manager.discovery.MaintainConsoleRegistryClientDiscovery;
import io.github.chenyilei2016.maintain.manager.exceptions.CommonException;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ScriptExecutionTaskDO;
import io.github.chenyilei2016.maintain.manager.pojo.entity.*;
import io.github.chenyilei2016.maintain.manager.pojo.mapper.ScriptExecutionTaskMapper;
import io.github.chenyilei2016.maintain.manager.pojo.repository.ScriptExecutionHistoryRepository;
import io.github.chenyilei2016.maintain.manager.pojo.vo.ScriptVO;
import io.github.chenyilei2016.maintain.manager.utils.IdUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class ScriptExecutionTaskService {
    private static final TypeReference<List<ScriptExecutionTargetResult>> TARGETS_TYPE = new TypeReference<>() {
    };

    private final ScriptExecutionTaskMapper taskMapper;
    private final ScriptExecutionHistoryRepository executionHistoryRepository;
    private final ExecutionApprovalService executionApprovalService;
    private final AuditLogService auditLogService;
    private final ExecutionRequestResolver executionRequestResolver;
    private final MaintainConsoleRegistryClientDiscovery registryClientDiscovery;
    private final ScriptInvoker scriptInvoker;
    private final ManagerProperties managerProperties;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final ThreadPoolTaskExecutor targetExecutor;
    private final Map<String, RunningTask> runningTasks = new ConcurrentHashMap<>();
    private final Map<String, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public ScriptExecutionTaskService(
            ScriptExecutionTaskMapper taskMapper,
            ScriptExecutionHistoryRepository executionHistoryRepository,
            ExecutionApprovalService executionApprovalService,
            AuditLogService auditLogService,
            ExecutionRequestResolver executionRequestResolver,
            MaintainConsoleRegistryClientDiscovery registryClientDiscovery,
            ScriptInvoker scriptInvoker,
            ManagerProperties managerProperties,
            @Qualifier("executionTaskExecutor") ThreadPoolTaskExecutor taskExecutor,
            @Qualifier("executionTargetExecutor") ThreadPoolTaskExecutor targetExecutor
    ) {
        this.taskMapper = taskMapper;
        this.executionHistoryRepository = executionHistoryRepository;
        this.executionApprovalService = executionApprovalService;
        this.auditLogService = auditLogService;
        this.executionRequestResolver = executionRequestResolver;
        this.registryClientDiscovery = registryClientDiscovery;
        this.scriptInvoker = scriptInvoker;
        this.managerProperties = managerProperties;
        this.taskExecutor = taskExecutor;
        this.targetExecutor = targetExecutor;
    }

    @PostConstruct
    public void recoverInterruptedTasks() {
        int recovered = taskMapper.update(null, Wrappers.<ScriptExecutionTaskDO>lambdaUpdate()
                .in(ScriptExecutionTaskDO::getStatus, ScriptExecutionTaskStatus.QUEUED.name(),
                        ScriptExecutionTaskStatus.RUNNING.name(), ScriptExecutionTaskStatus.CANCELLING.name())
                .set(ScriptExecutionTaskDO::getStatus, ScriptExecutionTaskStatus.FAILED.name())
                .set(ScriptExecutionTaskDO::getErrorMessage, "Manager 重启，上一次执行已中断")
                .set(ScriptExecutionTaskDO::getEndTime, LocalDateTime.now()));
        if (recovered > 0) {
            log.warn("已将 {} 个重启前未完成的执行任务标记为失败", recovered);
        }
    }

    @Transactional
    public ScriptExecutionTask submit(ExecutionTaskCreateWebRequest request, LocalLoginUser user) {
        ExecutionRequestResolver.ResolvedExecution execution = executionRequestResolver.resolve(request, user);
        ScriptVO scriptVO = execution.script();
        ScriptParameterSchema.ResolvedScript resolvedScript = execution.resolvedScript();
        ScriptTargetSelectionMode selectionMode = execution.selectionMode();
        List<ServiceInstance> selectedInstances = execution.instances();
        int timeoutSeconds = execution.timeoutSeconds();
        boolean production = executionApprovalService.isProduction(request.getEnv());
        String approvalId = executionApprovalService.consumeIfRequired(request, execution, user);

        List<ScriptExecutionTargetResult> targets = selectedInstances.stream()
                .map(instance -> new ScriptExecutionTargetResult()
                        .setInstance(ServiceInstanceDTO.from(instance))
                        .setStatus(ScriptExecutionTaskStatus.QUEUED))
                .toList();
        String taskId = IdUtil.generateSnowFlakeId();
        ScriptExecutionTaskDO task = new ScriptExecutionTaskDO()
                .setId(taskId)
                .setScriptId(scriptVO.getScript().getId())
                .setScriptName(scriptVO.getDirectoryNode().getName())
                .setServiceName(request.getService())
                .setEnvironment(request.getEnv())
                .setSelectionMode(selectionMode.name())
                .setRequestedInstanceId(request.getInstanceId())
                .setExecutorId(user.getEmployeeNo())
                .setExecutorName(user.getEmployeeName())
                .setScriptContent(request.getScript())
                .setFinalScriptContent(resolvedScript.persistedContent())
                .setParameters(resolvedScript.persistedParameters())
                .setStatus(ScriptExecutionTaskStatus.QUEUED.name())
                .setTargetsJson(JSON.toJSONString(targets))
                .setTimeoutSeconds(timeoutSeconds)
                .setCancelRequested(false)
                .setApprovalId(approvalId)
                .setProduction(production)
                .setCreateTime(LocalDateTime.now());
        taskMapper.insert(task);
        auditLogService.record(user, "SCRIPT_EXECUTION_SUBMIT", "SCRIPT_EXECUTION_TASK", taskId, "SUCCESS",
                Map.of("scriptId", task.getScriptId(), "service", task.getServiceName(),
                        "environment", task.getEnvironment(), "selectionMode", task.getSelectionMode(),
                        "targetCount", targets.size(), "production", production));

        RunningTask runningTask = new RunningTask();
        runningTasks.put(taskId, runningTask);
        try {
            taskExecutor.execute(() -> execute(task, selectedInstances, resolvedScript, runningTask));
        } catch (RuntimeException e) {
            runningTasks.remove(taskId);
            markPending(targets, ScriptExecutionTaskStatus.FAILED, "执行队列已满");
            finishTask(task, targets, ScriptExecutionTaskStatus.FAILED, "执行队列已满", System.currentTimeMillis());
            throw CommonException.createReminderException("执行队列已满，请稍后重试");
        }
        return toEntity(taskMapper.selectById(taskId));
    }

    public ScriptExecutionTask getTask(String taskId, LocalLoginUser user) {
        ScriptExecutionTaskDO task = requireTask(taskId);
        if (!Objects.equals(task.getExecutorId(), user.getEmployeeNo())
                && !managerProperties.getGlobalWhiteEmployeeNoList().contains(user.getEmployeeNo())) {
            throw CommonException.createReminderException("无权查看此执行任务");
        }
        return toEntity(task);
    }

    public ScriptExecutionTask cancel(String taskId, LocalLoginUser user) {
        ScriptExecutionTask task = getTask(taskId, user);
        if (task.isTerminal()) {
            return task;
        }
        int updated = taskMapper.update(null, Wrappers.<ScriptExecutionTaskDO>lambdaUpdate()
                .eq(ScriptExecutionTaskDO::getId, taskId)
                .in(ScriptExecutionTaskDO::getStatus, ScriptExecutionTaskStatus.QUEUED.name(),
                        ScriptExecutionTaskStatus.RUNNING.name(), ScriptExecutionTaskStatus.CANCELLING.name())
                .set(ScriptExecutionTaskDO::getCancelRequested, true)
                .set(ScriptExecutionTaskDO::getStatus, ScriptExecutionTaskStatus.CANCELLING.name()));
        if (updated == 0) {
            return toEntity(requireTask(taskId));
        }
        RunningTask runningTask = runningTasks.get(taskId);
        if (runningTask != null) {
            runningTask.cancelRequested.set(true);
            runningTask.targetFutures.forEach(future -> future.cancel(true));
        }
        auditLogService.record(user, "SCRIPT_EXECUTION_CANCEL", "SCRIPT_EXECUTION_TASK", taskId, "SUCCESS",
                Map.of("previousStatus", task.getStatus().name()));
        publish(taskId);
        return toEntity(requireTask(taskId));
    }

    public SseEmitter subscribe(String taskId, LocalLoginUser user) {
        ScriptExecutionTask task = getTask(taskId, user);
        SseEmitter emitter = new SseEmitter(managerProperties.getExecution().getSseTimeoutSeconds() * 1000L);
        Set<SseEmitter> taskEmitters = emitters.computeIfAbsent(taskId, ignored -> ConcurrentHashMap.newKeySet());
        taskEmitters.add(emitter);
        Runnable cleanup = () -> removeEmitter(taskId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(ignored -> cleanup.run());
        send(emitter, task);
        if (task.isTerminal()) {
            emitter.complete();
        }
        return emitter;
    }

    public List<ServiceInstanceDTO> listInstances(String serviceName, String environment) {
        return registryClientDiscovery.listServiceInstances(serviceName, environment).stream()
                .limit(managerProperties.getExecution().getMaxTargets())
                .map(ServiceInstanceDTO::from)
                .toList();
    }

    private void execute(
            ScriptExecutionTaskDO task,
            List<ServiceInstance> instances,
            ScriptParameterSchema.ResolvedScript resolvedScript,
            RunningTask runningTask
    ) {
        long startMillis = System.currentTimeMillis();
        List<ScriptExecutionTargetResult> targets = parseTargets(task.getTargetsJson());
        try {
            if (runningTask.cancelRequested.get()) {
                markPending(targets, ScriptExecutionTaskStatus.CANCELLED, "执行已取消");
                finishTask(task, targets, ScriptExecutionTaskStatus.CANCELLED, null, startMillis);
                return;
            }
            taskMapper.updateById(new ScriptExecutionTaskDO()
                    .setId(task.getId())
                    .setStatus(ScriptExecutionTaskStatus.RUNNING.name())
                    .setStartTime(LocalDateTime.now()));
            targets.forEach(target -> target.setStatus(ScriptExecutionTaskStatus.RUNNING));
            persistTargets(task.getId(), targets);
            publish(task.getId());

            CompletionService<IndexedTargetResult> completionService = new ExecutorCompletionService<>(
                    targetExecutor.getThreadPoolExecutor());
            for (int index = 0; index < instances.size(); index++) {
                int targetIndex = index;
                Future<IndexedTargetResult> future = completionService.submit(() -> new IndexedTargetResult(
                        targetIndex, executeTarget(task, instances.get(targetIndex), resolvedScript)));
                runningTask.targetFutures.add(future);
            }

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(task.getTimeoutSeconds());
            int completed = 0;
            boolean timedOut = false;
            while (completed < instances.size() && !runningTask.cancelRequested.get()) {
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    timedOut = true;
                    break;
                }
                Future<IndexedTargetResult> future = completionService.poll(remaining, TimeUnit.NANOSECONDS);
                if (future == null) {
                    timedOut = true;
                    break;
                }
                try {
                    IndexedTargetResult result = future.get();
                    targets.set(result.index(), result.result());
                    completed++;
                    persistTargets(task.getId(), targets);
                    publish(task.getId());
                } catch (CancellationException ignored) {
                    break;
                } catch (ExecutionException e) {
                    throw new IllegalStateException("目标实例执行异常", e.getCause());
                }
            }

            if (runningTask.cancelRequested.get()) {
                runningTask.targetFutures.forEach(future -> future.cancel(true));
                markPending(targets, ScriptExecutionTaskStatus.CANCELLED, "已请求取消，未完成结果已废弃");
                finishTask(task, targets, ScriptExecutionTaskStatus.CANCELLED, null, startMillis);
            } else if (timedOut) {
                runningTask.targetFutures.forEach(future -> future.cancel(true));
                markPending(targets, ScriptExecutionTaskStatus.TIMED_OUT, "执行超时");
                finishTask(task, targets, ScriptExecutionTaskStatus.TIMED_OUT, "执行超时", startMillis);
            } else {
                finishTask(task, targets, ScriptExecutionTaskStatus.aggregate(targets), null, startMillis);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markPending(targets, ScriptExecutionTaskStatus.CANCELLED, "执行已中断");
            finishTask(task, targets, ScriptExecutionTaskStatus.CANCELLED, null, startMillis);
        } catch (RuntimeException e) {
            runningTask.targetFutures.forEach(future -> future.cancel(true));
            log.error("执行任务失败, taskId:{}", task.getId(), e);
            markPending(targets, ScriptExecutionTaskStatus.FAILED, e.getMessage());
            finishTask(task, targets, ScriptExecutionTaskStatus.FAILED, e.getMessage(), startMillis);
        } finally {
            runningTasks.remove(task.getId());
        }
    }

    private ScriptExecutionTargetResult executeTarget(
            ScriptExecutionTaskDO task,
            ServiceInstance instance,
            ScriptParameterSchema.ResolvedScript resolvedScript
    ) {
        long startMillis = System.currentTimeMillis();
        ScriptExecutionTargetResult target = new ScriptExecutionTargetResult().setInstance(ServiceInstanceDTO.from(instance));
        try {
            ApiResult<InvokeScriptResultDTO> result = scriptInvoker.invoke(task.getServiceName(), task.getEnvironment(),
                    instance, resolvedScript.executableContent(), resolvedScript);
            if (result == null || !result.isSuccess()) {
                return target.setStatus(ScriptExecutionTaskStatus.FAILED)
                        .setErrorMessage(result == null ? "客户端未返回结果" : result.getMsg())
                        .setDuration(durationSince(startMillis));
            }
            return target.setStatus(ScriptExecutionTaskStatus.SUCCESS)
                    .setResult(ScriptExecutionResult.fromRaw(result.getData().getScriptResult()))
                    .setDuration(durationSince(startMillis));
        } catch (RuntimeException e) {
            return target.setStatus(Thread.currentThread().isInterrupted()
                            ? ScriptExecutionTaskStatus.CANCELLED
                            : ScriptExecutionTaskStatus.FAILED)
                    .setErrorMessage(e.getMessage())
                    .setDuration(durationSince(startMillis));
        }
    }

    private void persistTargets(String taskId, List<ScriptExecutionTargetResult> targets) {
        taskMapper.updateById(new ScriptExecutionTaskDO()
                .setId(taskId)
                .setTargetsJson(JSON.toJSONString(targets)));
    }

    private void finishTask(
            ScriptExecutionTaskDO task,
            List<ScriptExecutionTargetResult> targets,
            ScriptExecutionTaskStatus status,
            String errorMessage,
            long startMillis
    ) {
        taskMapper.updateById(new ScriptExecutionTaskDO()
                .setId(task.getId())
                .setStatus(status.name())
                .setTargetsJson(JSON.toJSONString(targets))
                .setErrorMessage(errorMessage)
                .setEndTime(LocalDateTime.now())
                .setDuration(durationSince(startMillis)));
        recordExecutionHistory(task, targets, status, errorMessage, startMillis);
        LocalLoginUser actor = new LocalLoginUser();
        actor.setEmployeeNo(task.getExecutorId());
        actor.setEmployeeName(task.getExecutorName());
        auditLogService.record(actor, "SCRIPT_EXECUTION_FINISH", "SCRIPT_EXECUTION_TASK", task.getId(),
                status.name(), Map.of("scriptId", task.getScriptId(), "targetCount", targets.size(),
                        "duration", durationSince(startMillis)));
        publish(task.getId());
    }

    private void recordExecutionHistory(
            ScriptExecutionTaskDO task,
            List<ScriptExecutionTargetResult> targets,
            ScriptExecutionTaskStatus status,
            String errorMessage,
            long startMillis
    ) {
        try {
            ScriptExecutionResult resultPayload = targets.size() == 1 && targets.getFirst().getResult() != null
                    ? targets.getFirst().getResult()
                    : ScriptExecutionResult.fromRaw(JSON.toJSONString(Map.of(
                    "taskId", task.getId(), "status", status, "targets", targets)));
            long endMillis = System.currentTimeMillis();
            executionHistoryRepository.save(new ScriptExecutionHistoryEntity()
                    .setId(task.getId())
                    .setScriptId(task.getScriptId())
                    .setScriptName(task.getScriptName())
                    .setServiceName(task.getServiceName())
                    .setExecutorId(task.getExecutorId())
                    .setExecutorName(task.getExecutorName())
                    .setScriptContent(task.getScriptContent())
                    .setFinalScriptContent(task.getFinalScriptContent())
                    .setParameters(task.getParameters())
                    .setResult(resultPayload.primaryText())
                    .setProtocolVersion(resultPayload.getProtocolVersion())
                    .setResultPayload(resultPayload.toJson())
                    .setStatus(status == ScriptExecutionTaskStatus.SUCCESS ? "success" : "error")
                    .setErrorMessage(errorMessage)
                    .setStartTime(new Timestamp(startMillis).toLocalDateTime())
                    .setEndTime(new Timestamp(endMillis).toLocalDateTime())
                    .setDuration(durationSince(startMillis))
                    .setCreateTime(LocalDateTime.now()));
        } catch (RuntimeException e) {
            log.error("保存异步执行历史失败, taskId:{}", task.getId(), e);
        }
    }

    private void markPending(
            List<ScriptExecutionTargetResult> targets,
            ScriptExecutionTaskStatus status,
            String errorMessage
    ) {
        targets.stream()
                .filter(target -> target.getStatus() == ScriptExecutionTaskStatus.QUEUED
                        || target.getStatus() == ScriptExecutionTaskStatus.RUNNING)
                .forEach(target -> target.setStatus(status).setErrorMessage(errorMessage));
    }

    private void publish(String taskId) {
        Set<SseEmitter> taskEmitters = emitters.get(taskId);
        if (taskEmitters == null || taskEmitters.isEmpty()) {
            return;
        }
        ScriptExecutionTask task = toEntity(requireTask(taskId));
        taskEmitters.forEach(emitter -> send(emitter, task));
        if (task.isTerminal()) {
            taskEmitters.forEach(SseEmitter::complete);
            emitters.remove(taskId);
        }
    }

    @Scheduled(fixedDelayString = "${maintain.manager.execution.sse-heartbeat-millis:15000}")
    public void sendSseHeartbeat() {
        emitters.forEach((taskId, taskEmitters) -> taskEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (IOException | IllegalStateException e) {
                removeEmitter(taskId, emitter);
            }
        }));
    }

    private void send(SseEmitter emitter, ScriptExecutionTask task) {
        try {
            emitter.send(SseEmitter.event().id(task.getId() + '-' + System.nanoTime()).data(task));
        } catch (IOException | IllegalStateException e) {
            removeEmitter(task.getId(), emitter);
        }
    }

    private void removeEmitter(String taskId, SseEmitter emitter) {
        Set<SseEmitter> taskEmitters = emitters.get(taskId);
        if (taskEmitters != null) {
            taskEmitters.remove(emitter);
            if (taskEmitters.isEmpty()) {
                emitters.remove(taskId, taskEmitters);
            }
        }
    }

    private ScriptExecutionTaskDO requireTask(String taskId) {
        ScriptExecutionTaskDO task = taskMapper.selectById(taskId);
        if (task == null) {
            throw CommonException.createReminderException("执行任务不存在");
        }
        return task;
    }

    private ScriptExecutionTask toEntity(ScriptExecutionTaskDO task) {
        ScriptExecutionTaskStatus status = ScriptExecutionTaskStatus.valueOf(task.getStatus());
        List<ScriptExecutionTargetResult> targets = parseTargets(task.getTargetsJson());
        if (status.isTerminal() && status != ScriptExecutionTaskStatus.SUCCESS
                && status != ScriptExecutionTaskStatus.PARTIAL_SUCCESS) {
            markPending(targets, status, task.getErrorMessage());
        }
        return new ScriptExecutionTask()
                .setId(task.getId())
                .setScriptId(task.getScriptId())
                .setScriptName(task.getScriptName())
                .setServiceName(task.getServiceName())
                .setEnvironment(task.getEnvironment())
                .setSelectionMode(ScriptTargetSelectionMode.valueOf(task.getSelectionMode()))
                .setRequestedInstanceId(task.getRequestedInstanceId())
                .setExecutorId(task.getExecutorId())
                .setExecutorName(task.getExecutorName())
                .setStatus(status)
                .setTargets(targets)
                .setTimeoutSeconds(task.getTimeoutSeconds())
                .setCancelRequested(Boolean.TRUE.equals(task.getCancelRequested()))
                .setErrorMessage(task.getErrorMessage())
                .setCreateTime(task.getCreateTime())
                .setStartTime(task.getStartTime())
                .setEndTime(task.getEndTime())
                .setDuration(task.getDuration())
                .setApprovalId(task.getApprovalId())
                .setProduction(Boolean.TRUE.equals(task.getProduction()));
    }

    private List<ScriptExecutionTargetResult> parseTargets(String targetsJson) {
        return targetsJson == null ? new ArrayList<>() : new ArrayList<>(JSON.parseObject(targetsJson, TARGETS_TYPE));
    }

    private int durationSince(long startMillis) {
        return Math.toIntExact(System.currentTimeMillis() - startMillis);
    }

    private record IndexedTargetResult(int index, ScriptExecutionTargetResult result) {
    }

    private static final class RunningTask {
        private final AtomicBoolean cancelRequested = new AtomicBoolean();
        private final List<Future<?>> targetFutures = new CopyOnWriteArrayList<>();
    }
}
