package io.github.chenyilei2016.maintain.manager.config;

import io.github.chenyilei2016.maintain.manager.security.ConsoleRoleInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author chenyilei
 * @since 2024/05/11 15:59
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final ConsoleRoleInterceptor roleInterceptor;

    public WebMvcConfig(ConsoleRoleInterceptor roleInterceptor) {
        this.roleInterceptor = roleInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(roleInterceptor).addPathPatterns("/manager/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry
                .addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/")
        ;
    }
}
