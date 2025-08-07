package io.github.chenyilei2016.maintain.client.registry.configuration;


import io.github.chenyilei2016.maintain.client.common.utils.LogUtil;
import io.github.chenyilei2016.maintain.client.registry.metadata.MaintainConsoleMetadataPostProcessor;
import io.github.chenyilei2016.maintain.client.registry.properties.MaintainConsoleRegistryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationAutoConfiguration;
import org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationConfiguration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistryAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @see ServiceRegistryAutoConfiguration
 */
@Configuration
@AutoConfigureBefore(value = {
        AutoServiceRegistrationConfiguration.class
        , AutoServiceRegistrationAutoConfiguration.class
})
@ConditionalOnClass(name = {
        "org.springframework.cloud.client.discovery.DiscoveryClient",
        "org.springframework.cloud.client.serviceregistry.ServiceRegistry"
})
@ConditionalOnProperty(value = MaintainConsoleRegistryProperties.KEY_REGISTRY_ENABLED, havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(value = MaintainConsoleRegistryProperties.class)
public class MaintainConsoleClientRegistryAutoConfiguration {
    Logger log = LoggerFactory.getLogger(MaintainConsoleClientRegistryAutoConfiguration.class);

    /**
     * maintain console 注册元信息
     */
    @Bean
    public MaintainConsoleMetadataPostProcessor maintainConsoleMetadataPostProcessor(MaintainConsoleRegistryProperties properties) {
        LogUtil.info(log, "MaintainConsoleMetadataPostProcessor init");
        return new MaintainConsoleMetadataPostProcessor(properties);
    }

}
