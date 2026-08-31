package io.github.chenyilei2016.maintain.manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ExecutionConfiguration {

    @Bean
    public ThreadPoolTaskExecutor executionTargetExecutor(ManagerProperties properties) {
        ManagerProperties.Execution execution = properties.getExecution();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("maintain-invoke-");
        executor.setCorePoolSize(execution.getTargetCorePoolSize());
        executor.setMaxPoolSize(execution.getTargetMaxPoolSize());
        executor.setQueueCapacity(execution.getTargetQueueCapacity());
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }
}
