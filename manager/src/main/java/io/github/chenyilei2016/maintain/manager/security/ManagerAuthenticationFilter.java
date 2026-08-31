package io.github.chenyilei2016.maintain.manager.security;

import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import io.github.chenyilei2016.maintain.manager.context.LoginUserContext;
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
@Profile("!local")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ManagerAuthenticationFilter extends OncePerRequestFilter {
    private final TrustedIdentityVerifier identityVerifier;

    public ManagerAuthenticationFilter(ManagerProperties properties) {
        this.identityVerifier = new TrustedIdentityVerifier(properties.getSecurity());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/") || uri.equals("/index.html") || uri.equals("/favicon.ico")
                || uri.equals("/workspace") || uri.startsWith("/workspace/") || uri.startsWith("/tools/")
                || uri.startsWith("/static/") || uri.equals("/actuator/health") || uri.equals("/actuator/info");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            LoginUserContext.setUser(identityVerifier.verify(
                    request.getHeader("X-Maintain-User-Id"),
                    request.getHeader("X-Maintain-User-Name"),
                    request.getHeader("X-Maintain-User-Roles"),
                    request.getHeader("X-Maintain-Identity-Timestamp"),
                    request.getHeader("X-Maintain-Identity-Nonce"),
                    request.getHeader("X-Maintain-Identity-Signature"),
                    request.getMethod(), request.getRequestURI()));
            filterChain.doFilter(request, response);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"success\":false,\"msg\":\"身份认证失败\",\"code\":401}");
        } finally {
            LoginUserContext.remove();
        }
    }
}
