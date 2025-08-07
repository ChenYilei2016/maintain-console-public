package cn.chenyilei.maintain.client.http.configuration;

import cn.chenyilei.maintain.client.common.console.IMaintainConsoleExecutor;
import cn.chenyilei.maintain.client.common.utils.LogUtil;
import cn.chenyilei.maintain.client.http.api.HttpMaintainConsoleClientApiImplFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import java.util.EnumSet;

import static javax.servlet.DispatcherType.REQUEST;

/**
 * @author chenyilei
 * @since 2024/05/17 11:07
 */
@Configuration
public class MaintainConsoleClientHttpAutoConfiguration {
    Logger log = LoggerFactory.getLogger(MaintainConsoleClientHttpAutoConfiguration.class);

    @Bean
    @ConditionalOnClass({Servlet.class, DispatcherServlet.class})
    public FilterRegistrationBean<Filter> maintainConsoleHttpApiImplFilter(IMaintainConsoleExecutor maintainConsoleExecutor) {
        LogUtil.info(log, "HttpMaintainConsoleClientApiImplFilter init");
        FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
        Filter httpMaintainConsoleClientApiImplFilter = new HttpMaintainConsoleClientApiImplFilter(maintainConsoleExecutor);
        filterRegistrationBean.setFilter(httpMaintainConsoleClientApiImplFilter);
        filterRegistrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        filterRegistrationBean.setDispatcherTypes(EnumSet.of(REQUEST));
        return filterRegistrationBean;
    }
}
