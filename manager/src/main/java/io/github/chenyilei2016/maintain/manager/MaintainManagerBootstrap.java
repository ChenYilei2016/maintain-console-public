package io.github.chenyilei2016.maintain.manager;

import io.github.chenyilei2016.maintain.client.groovy.configuration.MaintainConsoleGroovyProperties;
import io.github.chenyilei2016.maintain.client.groovy.execute.GroovyMaintainConsoleExecutor;
import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(value = {ManagerProperties.class, MaintainConsoleGroovyProperties.class})
public class MaintainManagerBootstrap {

    @Bean
    @Profile({"local", "demo"})
    public GroovyMaintainConsoleExecutor localGroovyMaintainConsoleExecutor(MaintainConsoleGroovyProperties properties) {
        return new GroovyMaintainConsoleExecutor(properties);
    }

    public static void main(String[] args) {
        SpringApplication.run(MaintainManagerBootstrap.class, args);
        log.info("Maintain Console Manager started");
    }

}
