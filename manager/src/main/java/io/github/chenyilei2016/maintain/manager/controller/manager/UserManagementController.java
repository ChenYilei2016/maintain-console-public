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
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/manager/admin/users")
@RequireConsoleRole(ConsoleRole.ADMIN)
@RequiredArgsConstructor
public class UserManagementController {
    private final ConsoleUserService users;

    @GetMapping
    public AjaxPageResult<List<ConsoleUserService.UserView>> page(@RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "20") int size) {
        IPage<ConsoleUserDO> result = users.page(page, size);
        return new AjaxPageResult<>(true, result.getRecords().stream().map(users::view).toList(), null,
                (int) result.getCurrent(), (int) result.getSize(), result.getTotal());
    }

    @PostMapping("/{id}")
    public AjaxResult<Boolean> update(@PathVariable String id, @RequestBody @Valid UserUpdate request) {
        users.update(id, request.status(), request.roles(), LoginUserContext.getUser());
        return AjaxResult.success(true);
    }

    public record UserUpdate(ConsoleUserStatus status, @Size(max = 3) Set<ConsoleRole> roles) {
        public UserUpdate {
            if (status == null || roles == null) throw new IllegalArgumentException("用户状态和角色不能为空");
        }
    }
}
