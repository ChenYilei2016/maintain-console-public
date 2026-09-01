package io.github.chenyilei2016.maintain.manager.controller.manager;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.github.chenyilei2016.maintain.manager.constant.ConsoleRole;
import io.github.chenyilei2016.maintain.manager.context.LoginUserContext;
import io.github.chenyilei2016.maintain.manager.identity.ConsoleUserService;
import io.github.chenyilei2016.maintain.manager.identity.ConsoleUserStatus;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxPageResult;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxResult;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ConsoleUserDO;
import io.github.chenyilei2016.maintain.manager.security.RequireConsoleRole;
import io.github.chenyilei2016.maintain.manager.service.AuditLogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/manager/admin/users")
@RequireConsoleRole(ConsoleRole.ADMIN)
@RequiredArgsConstructor
public class UserManagementController {
    private final ConsoleUserService users;
    private final AuditLogService audit;

    @GetMapping
    public AjaxPageResult<List<ConsoleUserService.UserView>> page(@RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        IPage<ConsoleUserDO> result = users.page(page, size);
        return new AjaxPageResult<>(true, result.getRecords().stream().map(users::view).toList(), null,
                (int) result.getCurrent(), (int) result.getSize(), result.getTotal());
    }

    @PostMapping
    public AjaxResult<ConsoleUserService.UserView> create(@RequestBody @Valid UserCreate request) {
        ConsoleUserService.UserView created = users.createLocalUser(request.username(), request.displayName(),
                request.initialPassword(), request.roles());
        audit.record(LoginUserContext.getUser(), "USER_CREATE", "USER", created.id(), "SUCCESS",
                Map.of("username", created.employeeNo(), "status", created.status(), "roles", created.roles()));
        return AjaxResult.success(created);
    }

    @PostMapping("/{id}")
    public AjaxResult<Boolean> update(@PathVariable String id, @RequestBody @Valid UserUpdate request) {
        users.update(id, request.status(), request.roles(), LoginUserContext.getUser());
        audit.record(LoginUserContext.getUser(), "USER_UPDATE", "USER", id, "SUCCESS",
                Map.of("status", request.status(), "roles", request.roles()));
        return AjaxResult.success(true);
    }

    @PostMapping("/{id}/password")
    public AjaxResult<Boolean> resetPassword(@PathVariable String id, @RequestBody @Valid PasswordReset request) {
        users.resetLocalPassword(id, request.newPassword());
        audit.record(LoginUserContext.getUser(), "USER_PASSWORD_RESET", "USER", id, "SUCCESS", Map.of());
        return AjaxResult.success(true);
    }

    public record UserCreate(@NotBlank @Size(max = 64) String username,
                             @NotBlank @Size(max = 64) String displayName,
                             @NotBlank @Size(max = 128) String initialPassword,
                             Set<ConsoleRole> roles) {
        public UserCreate {
            if (roles == null) roles = Set.of();
        }
    }

    public record UserUpdate(ConsoleUserStatus status, Set<ConsoleRole> roles) {
        public UserUpdate {
            if (status == null || roles == null) throw new IllegalArgumentException("用户状态和角色不能为空");
        }
    }

    public record PasswordReset(@NotBlank @Size(max = 128) String newPassword) {
    }
}
