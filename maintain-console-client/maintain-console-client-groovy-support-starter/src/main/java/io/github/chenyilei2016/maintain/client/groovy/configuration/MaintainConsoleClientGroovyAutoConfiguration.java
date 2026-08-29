package io.github.chenyilei2016.maintain.client.groovy.configuration;

import io.github.chenyilei2016.maintain.client.common.utils.LogUtil;
import io.github.chenyilei2016.maintain.client.groovy.execute.GroovyMaintainConsoleExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author chenyilei
 * @since 2024/05/20 14:41
 */
@Configuration
@EnableConfigurationProperties(MaintainConsoleGroovyProperties.class)
public class MaintainConsoleClientGroovyAutoConfiguration {

    Logger logger = LoggerFactory.getLogger(MaintainConsoleClientGroovyAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(GroovyMaintainConsoleExecutor.class)
    public GroovyMaintainConsoleExecutor groovyMaintainConsoleExecutor(MaintainConsoleGroovyProperties properties) {
        LogUtil.info(logger, "GroovyMaintainConsoleExecutor init");
        return new GroovyMaintainConsoleExecutor(properties);
    }
}
