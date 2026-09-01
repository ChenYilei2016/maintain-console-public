package io.github.chenyilei2016.maintain.manager.security;

import io.github.chenyilei2016.maintain.manager.context.LoginUserContext;
import io.github.chenyilei2016.maintain.manager.identity.ConsoleSession;
import io.github.chenyilei2016.maintain.manager.identity.ConsoleUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Profile({"local", "demo"})
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LocalAuthenticationFilter extends OncePerRequestFilter {
    private final ConsoleUserService users;

    public LocalAuthenticationFilter(ConsoleUserService users) {
        this.users = users;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/") || uri.equals("/index.html") || uri.equals("/favicon.ico") || uri.equals("/login")
                || uri.equals("/workspace") || uri.startsWith("/workspace/") || uri.startsWith("/tools/")
                || uri.startsWith("/admin") || uri.startsWith("/static/") || uri.startsWith("/manager/auth/")
                || uri.equals("/actuator/health") || uri.equals("/actuator/info");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        var session = request.getSession(false);
        Object userId = session == null ? null : session.getAttribute(ConsoleSession.USER_ID);
        if (!(userId instanceof String id) || id.isBlank()) {
            unauthorized(response);
            return;
        }
        try {
            LoginUserContext.setUser(users.requireActive(id));
        } catch (RuntimeException invalidSession) {
            session.invalidate();
            unauthorized(response);
            return;
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            LoginUserContext.remove();
        }
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"success\":false,\"msg\":\"请先登录\",\"code\":401}");
    }
}
