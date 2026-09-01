package io.github.chenyilei2016.maintain.manager.controller;

import io.github.chenyilei2016.maintain.manager.constant.ConsoleRole;
import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;
import io.github.chenyilei2016.maintain.manager.controller.dto.res.AuthenticationStateWebResponse;
import io.github.chenyilei2016.maintain.manager.identity.*;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxResult;
import io.github.chenyilei2016.maintain.manager.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manager/auth")
@Profile({"local", "demo"})
@RequiredArgsConstructor
public class AuthenticationController {
    private final MockLoginProvider provider;
    private final ConsoleUserService users;
    private final AuditLogService audit;

    @GetMapping("/state")
    public AjaxResult<AuthenticationStateWebResponse> state(HttpServletRequest request, CsrfToken csrfToken) {
        boolean authenticated = false;
        var session = request.getSession(false);
        Object userId = session == null ? null : session.getAttribute(ConsoleSession.USER_ID);
        if (userId instanceof String id) {
            try {
                users.requireActive(id);
                authenticated = true;
            } catch (RuntimeException invalidSession) {
                session.invalidate();
            }
        }
        return AjaxResult.success(new AuthenticationStateWebResponse(authenticated,
                AuthenticationProviderType.MOCK_SDK, csrfToken.getToken(), MockLoginAccount.options()));
    }

    @PostMapping("/login")
    public AjaxResult<String> login(@RequestBody @Valid LoginRequest request, HttpServletRequest servletRequest) {
        LocalLoginUser user = users.login(provider.authenticate(request.accountId()));
        var session = servletRequest.getSession(true);
        servletRequest.changeSessionId();
        session.setAttribute(ConsoleSession.USER_ID, user.getId());
        audit.record(user, "USER_LOGIN", "USER", user.getId(), "SUCCESS", java.util.Map.of("provider", "MOCK_SDK"));
        return AjaxResult.success(safeReturnTo(request.returnTo(), user), "登录成功");
    }

    @PostMapping("/logout")
    public AjaxResult<Boolean> logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        return AjaxResult.success(true);
    }

    private String safeReturnTo(String returnTo, LocalLoginUser user) {
        if (returnTo == null || returnTo.isBlank() || returnTo.equals("/")) {
            if (ConsoleRole.ADMIN.grantedTo(user)) return "/admin";
            return ConsoleRole.DEVELOPER.grantedTo(user) ? "/workspace" : "/";
        }
        if (!returnTo.startsWith("/") || returnTo.startsWith("//") || returnTo.contains("\\")
                || returnTo.contains("\r") || returnTo.contains("\n")) {
            throw new IllegalArgumentException("登录返回地址不安全");
        }
        return returnTo;
    }

    public record LoginRequest(@NotBlank @Size(max = 64) String accountId, @Size(max = 2048) String returnTo) {
    }
}
