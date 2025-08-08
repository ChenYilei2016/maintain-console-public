package io.github.chenyilei2016.maintain.manager;

import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
@SpringBootApplication
@MapperScan(basePackages = "io.github.chenyilei2016.maintain.manager.pojo.mapper")
@EnableDiscoveryClient
@EnableConfigurationProperties(value = ManagerProperties.class)
public class MaintainManagerBootstrap {


    public static void main(String[] args) {
        /**
         * 本地数据库 + 本地注册中心
         */
//        System.setProperty("spring.profiles.active", "local");

        /**
         * 使用PROD配置文件, 默认需要补充nacos 和 数据的配置
         */
        System.setProperty("spring.profiles.active", "prod");
        ConfigurableApplicationContext run = SpringApplication.run(MaintainManagerBootstrap.class, args);
        log.info(">>>>>>>>>>>>> cyl spring boot start success !!!!  <<<<<<<<<<<<<<<");
    }


}
