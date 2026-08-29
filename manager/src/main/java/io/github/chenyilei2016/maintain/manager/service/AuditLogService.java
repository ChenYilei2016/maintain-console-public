package io.github.chenyilei2016.maintain.manager.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;
import io.github.chenyilei2016.maintain.manager.exceptions.CommonException;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.AuditLogDO;
import io.github.chenyilei2016.maintain.manager.pojo.mapper.AuditLogMapper;
import io.github.chenyilei2016.maintain.manager.utils.IdUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class AuditLogService {
    private static final int MAX_DETAILS_LENGTH = 16 * 1024;
    private final AuditLogMapper auditLogMapper;
    private final ManagerProperties managerProperties;

    public AuditLogService(AuditLogMapper auditLogMapper, ManagerProperties managerProperties) {
        this.auditLogMapper = auditLogMapper;
        this.managerProperties = managerProperties;
    }

    public IPage<AuditLogDO> page(
            LocalLoginUser user,
            int page,
            int size,
            String actorId,
            String action,
            String targetId
    ) {
        if (!user.getRoles().contains("ADMIN") && !user.getRoles().contains("AUDITOR")
                && !managerProperties.getGlobalWhiteEmployeeNoList().contains(user.getEmployeeNo())) {
            throw CommonException.createReminderException("当前用户没有审计日志查看权限");
        }
        int boundedSize = Math.max(1, Math.min(size, 100));
        return auditLogMapper.selectPage(new Page<>(Math.max(1, page), boundedSize),
                Wrappers.<AuditLogDO>lambdaQuery()
                        .eq(actorId != null && !actorId.isBlank(), AuditLogDO::getActorId, actorId)
                        .eq(action != null && !action.isBlank(), AuditLogDO::getAction, action)
                        .eq(targetId != null && !targetId.isBlank(), AuditLogDO::getTargetId, targetId)
                        .orderByDesc(AuditLogDO::getCreateTime));
    }

    public void record(
            LocalLoginUser actor,
            String action,
            String targetType,
            String targetId,
            String outcome,
            Map<String, ?> details
    ) {
        try {
            HttpServletRequest request = currentRequest();
            String serializedDetails = JSON.toJSONString(details == null ? Map.of() : details);
            if (serializedDetails.length() > MAX_DETAILS_LENGTH) {
                serializedDetails = serializedDetails.substring(0, MAX_DETAILS_LENGTH);
            }
            auditLogMapper.insert(new AuditLogDO()
                    .setId(IdUtil.generateSnowFlakeId())
                    .setActorId(actor.getEmployeeNo())
                    .setActorName(actor.getEmployeeName())
                    .setAction(action)
                    .setTargetType(targetType)
                    .setTargetId(targetId)
                    .setOutcome(outcome)
                    .setDetails(serializedDetails)
                    .setClientIp(request == null ? null : request.getRemoteAddr())
                    .setUserAgent(request == null ? null : truncate(request.getHeader("User-Agent"), 512))
                    .setCreateTime(LocalDateTime.now()));
        } catch (RuntimeException e) {
            log.error("写入审计日志失败, action:{}, targetId:{}", action, targetId, e);
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
