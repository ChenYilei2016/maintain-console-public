package io.github.chenyilei2016.maintain.manager.security;

import io.github.chenyilei2016.maintain.manager.context.LoginUserContext;
import io.github.chenyilei2016.maintain.manager.exceptions.CommonException;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ConsoleRoleInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                             jakarta.servlet.http.HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) return true;
        RequireConsoleRole required = AnnotatedElementUtils.findMergedAnnotation(method.getMethod(), RequireConsoleRole.class);
        if (required == null) {
            required = AnnotatedElementUtils.findMergedAnnotation(method.getBeanType(), RequireConsoleRole.class);
        }
        if (required != null && !required.value().grantedTo(LoginUserContext.getUser())) {
            throw CommonException.createReminderException("当前用户没有{}权限", required.value().name());
        }
        return true;
    }
}
