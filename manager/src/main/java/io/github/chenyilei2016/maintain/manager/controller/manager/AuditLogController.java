package io.github.chenyilei2016.maintain.manager.controller.manager;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.chenyilei2016.maintain.manager.context.LoginUserContext;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxPageResult;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.AuditLogDO;
import io.github.chenyilei2016.maintain.manager.service.AuditLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/manager/audit")
public class AuditLogController {
    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/logs")
    public AjaxPageResult<List<AuditLogDO>> logs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetId
    ) {
        IPage<AuditLogDO> result = auditLogService.page(
                LoginUserContext.getUser(), page, size, actorId, action, targetId);
        return new AjaxPageResult<>(true, result.getRecords(), null,
                (int) result.getCurrent(), (int) result.getSize(), result.getTotal());
    }
}
