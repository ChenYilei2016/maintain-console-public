package cn.chenyilei.maintain.client.groovy.configuration;

import cn.chenyilei.maintain.client.common.utils.LogUtil;
import cn.chenyilei.maintain.client.groovy.execute.GroovyMaintainConsoleExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author chenyilei
 * @since 2024/05/20 14:41
 */
@Configuration
public class MaintainConsoleClientGroovyAutoConfiguration {

    Logger logger = LoggerFactory.getLogger(MaintainConsoleClientGroovyAutoConfiguration.class);

    @Bean
    public GroovyMaintainConsoleExecutor groovyMaintainConsoleExecutor() {
        LogUtil.info(logger, "GroovyMaintainConsoleExecutor init");
        return new GroovyMaintainConsoleExecutor();
    }
}
