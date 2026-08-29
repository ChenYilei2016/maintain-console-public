package io.github.chenyilei2016.maintain.manager.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;
import io.github.chenyilei2016.maintain.manager.controller.dto.ExecutionApprovalCreateWebRequest;
import io.github.chenyilei2016.maintain.manager.controller.dto.ExecutionApprovalDecisionWebRequest;
import io.github.chenyilei2016.maintain.manager.controller.dto.ExecutionTaskCreateWebRequest;
import io.github.chenyilei2016.maintain.manager.exceptions.CommonException;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ExecutionApprovalDO;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ExecutionApproval;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ExecutionApprovalStatus;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptTargetSelectionMode;
import io.github.chenyilei2016.maintain.manager.pojo.mapper.ExecutionApprovalMapper;
import io.github.chenyilei2016.maintain.manager.utils.IdUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ExecutionApprovalService {
    private final ExecutionApprovalMapper approvalMapper;
    private final ExecutionRequestResolver executionRequestResolver;
    private final AuditLogService auditLogService;
    private final ManagerProperties managerProperties;
    private final EnvironmentCatalogService environmentCatalogService;

    public ExecutionApprovalService(
            ExecutionApprovalMapper approvalMapper,
            ExecutionRequestResolver executionRequestResolver,
            AuditLogService auditLogService,
            ManagerProperties managerProperties,
            EnvironmentCatalogService environmentCatalogService
    ) {
        this.approvalMapper = approvalMapper;
        this.executionRequestResolver = executionRequestResolver;
        this.auditLogService = auditLogService;
        this.managerProperties = managerProperties;
        this.environmentCatalogService = environmentCatalogService;
    }

    @Transactional
    public ExecutionApproval create(ExecutionApprovalCreateWebRequest request, LocalLoginUser requester) {
        if (!isProduction(request.getExecution().getEnv())) {
            throw CommonException.createReminderException("非生产环境无需申请执行审批");
        }
        if (request.getReason().length() > 1000) {
            throw CommonException.createReminderException("申请理由不能超过 1000 个字符");
        }
        ExecutionRequestResolver.ResolvedExecution execution = executionRequestResolver.resolve(
                request.getExecution(), requester);
        LocalDateTime now = LocalDateTime.now();
        ExecutionApprovalDO approval = new ExecutionApprovalDO()
                .setId(IdUtil.generateSnowFlakeId())
                .setRequestDigest(ExecutionApprovalBinding.digest(request.getExecution(), execution))
                .setScriptId(execution.script().getScript().getId())
                .setScriptName(execution.script().getDirectoryNode().getName())
                .setServiceName(request.getExecution().getService())
                .setEnvironment(request.getExecution().getEnv())
                .setSelectionMode(execution.selectionMode().name())
                .setRequestedInstanceId(request.getExecution().getInstanceId())
                .setRequesterId(requester.getEmployeeNo())
                .setRequesterName(requester.getEmployeeName())
                .setStatus(ExecutionApprovalStatus.PENDING.name())
                .setScriptContent(execution.resolvedScript().persistedContent())
                .setParameters(execution.resolvedScript().persistedParameters())
                .setReason(request.getReason())
                .setCreateTime(now)
                .setExpireTime(now.plusMinutes(managerProperties.getSecurity().getApprovalExpireMinutes()));
        approvalMapper.insert(approval);
        auditLogService.record(requester, "EXECUTION_APPROVAL_REQUEST", "EXECUTION_APPROVAL", approval.getId(),
                "SUCCESS", Map.of("scriptId", approval.getScriptId(), "service", approval.getServiceName(),
                        "selectionMode", approval.getSelectionMode()));
        return toEntity(approval);
    }

    public ExecutionApproval get(String approvalId, LocalLoginUser user) {
        ExecutionApprovalDO approval = requireApproval(approvalId);
        if (!Objects.equals(approval.getRequesterId(), user.getEmployeeNo()) && !canApprove(user)) {
            throw CommonException.createReminderException("无权查看此审批单");
        }
        expireIfNecessary(approval);
        return toEntity(approvalMapper.selectById(approvalId));
    }

    public List<ExecutionApproval> listMine(LocalLoginUser user) {
        return approvalMapper.selectList(Wrappers.<ExecutionApprovalDO>lambdaQuery()
                        .eq(ExecutionApprovalDO::getRequesterId, user.getEmployeeNo())
                        .orderByDesc(ExecutionApprovalDO::getCreateTime)
                        .last("LIMIT 50"))
                .stream().map(this::toEntity).toList();
    }

    public List<ExecutionApproval> listPending(LocalLoginUser approver) {
        requireApprover(approver);
        return approvalMapper.selectList(Wrappers.<ExecutionApprovalDO>lambdaQuery()
                        .eq(ExecutionApprovalDO::getStatus, ExecutionApprovalStatus.PENDING.name())
                        .gt(ExecutionApprovalDO::getExpireTime, LocalDateTime.now())
                        .orderByAsc(ExecutionApprovalDO::getCreateTime)
                        .last("LIMIT 100"))
                .stream().map(this::toEntity).toList();
    }

    @Transactional
    public ExecutionApproval decide(
            String approvalId,
            ExecutionApprovalDecisionWebRequest request,
            LocalLoginUser approver
    ) {
        requireApprover(approver);
        if (request.getComment().length() > 1000) {
            throw CommonException.createReminderException("审批意见不能超过 1000 个字符");
        }
        ExecutionApprovalDO approval = requireApproval(approvalId);
        expireIfNecessary(approval);
        if (!ExecutionApprovalStatus.PENDING.name().equals(approval.getStatus())) {
            throw CommonException.createReminderException("审批单当前状态不允许审批: {}", approval.getStatus());
        }
        if (Objects.equals(approval.getRequesterId(), approver.getEmployeeNo())) {
            throw CommonException.createReminderException("申请人不能审批自己的生产执行");
        }
        ExecutionApprovalStatus targetStatus = request.isApproved()
                ? ExecutionApprovalStatus.APPROVED
                : ExecutionApprovalStatus.REJECTED;
        int updated = approvalMapper.update(null, Wrappers.<ExecutionApprovalDO>lambdaUpdate()
                .eq(ExecutionApprovalDO::getId, approvalId)
                .eq(ExecutionApprovalDO::getStatus, ExecutionApprovalStatus.PENDING.name())
                .set(ExecutionApprovalDO::getStatus, targetStatus.name())
                .set(ExecutionApprovalDO::getApproverId, approver.getEmployeeNo())
                .set(ExecutionApprovalDO::getApproverName, approver.getEmployeeName())
                .set(ExecutionApprovalDO::getDecisionComment, request.getComment())
                .set(ExecutionApprovalDO::getDecisionTime, LocalDateTime.now()));
        if (updated != 1) {
            throw CommonException.createReminderException("审批单已被其他人处理");
        }
        auditLogService.record(approver, request.isApproved() ? "EXECUTION_APPROVE" : "EXECUTION_REJECT",
                "EXECUTION_APPROVAL", approvalId, "SUCCESS", Map.of("requesterId", approval.getRequesterId(),
                        "scriptId", approval.getScriptId()));
        return toEntity(approvalMapper.selectById(approvalId));
    }

    @Transactional
    public String consumeIfRequired(
            ExecutionTaskCreateWebRequest request,
            ExecutionRequestResolver.ResolvedExecution execution,
            LocalLoginUser requester
    ) {
        if (!isProduction(request.getEnv())) {
            return null;
        }
        String expectedConfirmation = ExecutionApprovalBinding.confirmationText(
                request.getService(), execution.script().getDirectoryNode().getName());
        if (!Objects.equals(expectedConfirmation, request.getProductionConfirmation())) {
            throw CommonException.createReminderException("生产执行确认文本不匹配，请输入: {}", expectedConfirmation);
        }
        ExecutionApprovalDO approval = requireApproval(request.getApprovalId());
        expireIfNecessary(approval);
        if (!ExecutionApprovalStatus.APPROVED.name().equals(approval.getStatus())) {
            throw CommonException.createReminderException("生产执行审批未通过或已被使用");
        }
        if (!Objects.equals(approval.getRequesterId(), requester.getEmployeeNo())) {
            throw CommonException.createReminderException("审批单不属于当前执行人");
        }
        if (!Objects.equals(approval.getRequestDigest(), ExecutionApprovalBinding.digest(request, execution))) {
            throw CommonException.createReminderException("脚本、参数或执行目标已变更，请重新申请审批");
        }
        int consumed = approvalMapper.update(null, Wrappers.<ExecutionApprovalDO>lambdaUpdate()
                .eq(ExecutionApprovalDO::getId, approval.getId())
                .eq(ExecutionApprovalDO::getStatus, ExecutionApprovalStatus.APPROVED.name())
                .set(ExecutionApprovalDO::getStatus, ExecutionApprovalStatus.CONSUMED.name())
                .set(ExecutionApprovalDO::getConsumedTime, LocalDateTime.now()));
        if (consumed != 1) {
            throw CommonException.createReminderException("审批单已被使用");
        }
        auditLogService.record(requester, "EXECUTION_APPROVAL_CONSUME", "EXECUTION_APPROVAL", approval.getId(),
                "SUCCESS", Map.of("scriptId", approval.getScriptId()));
        return approval.getId();
    }

    public boolean isProduction(String targetEnvironment) {
        return environmentCatalogService.isProduction(targetEnvironment);
    }

    private void expireIfNecessary(ExecutionApprovalDO approval) {
        if (ExecutionApprovalStatus.PENDING.name().equals(approval.getStatus())
                || ExecutionApprovalStatus.APPROVED.name().equals(approval.getStatus())) {
            if (approval.getExpireTime().isBefore(LocalDateTime.now())) {
                approvalMapper.update(null, Wrappers.<ExecutionApprovalDO>lambdaUpdate()
                        .eq(ExecutionApprovalDO::getId, approval.getId())
                        .in(ExecutionApprovalDO::getStatus, ExecutionApprovalStatus.PENDING.name(),
                                ExecutionApprovalStatus.APPROVED.name())
                        .set(ExecutionApprovalDO::getStatus, ExecutionApprovalStatus.EXPIRED.name()));
                approval.setStatus(ExecutionApprovalStatus.EXPIRED.name());
            }
        }
    }

    private boolean canApprove(LocalLoginUser user) {
        return user.getRoles().contains("ADMIN") || user.getRoles().contains("APPROVER")
                || managerProperties.getGlobalWhiteEmployeeNoList().contains(user.getEmployeeNo());
    }

    private void requireApprover(LocalLoginUser user) {
        if (!canApprove(user)) {
            throw CommonException.createReminderException("当前用户没有生产执行审批权限");
        }
    }

    private ExecutionApprovalDO requireApproval(String approvalId) {
        if (approvalId == null || approvalId.isBlank()) {
            throw CommonException.createReminderException("生产执行必须提供审批单");
        }
        ExecutionApprovalDO approval = approvalMapper.selectById(approvalId);
        if (approval == null) {
            throw CommonException.createReminderException("审批单不存在");
        }
        return approval;
    }

    private ExecutionApproval toEntity(ExecutionApprovalDO approval) {
        return new ExecutionApproval()
                .setId(approval.getId())
                .setScriptId(approval.getScriptId())
                .setScriptName(approval.getScriptName())
                .setServiceName(approval.getServiceName())
                .setEnvironment(approval.getEnvironment())
                .setSelectionMode(ScriptTargetSelectionMode.valueOf(approval.getSelectionMode()))
                .setRequestedInstanceId(approval.getRequestedInstanceId())
                .setRequesterId(approval.getRequesterId())
                .setRequesterName(approval.getRequesterName())
                .setStatus(ExecutionApprovalStatus.valueOf(approval.getStatus()))
                .setScriptContent(approval.getScriptContent())
                .setParameters(approval.getParameters())
                .setReason(approval.getReason())
                .setApproverId(approval.getApproverId())
                .setApproverName(approval.getApproverName())
                .setDecisionComment(approval.getDecisionComment())
                .setCreateTime(approval.getCreateTime())
                .setExpireTime(approval.getExpireTime())
                .setDecisionTime(approval.getDecisionTime())
                .setConsumedTime(approval.getConsumedTime())
                .setProductionConfirmation(ExecutionApprovalBinding.confirmationText(
                        approval.getServiceName(), approval.getScriptName()));
    }
}
