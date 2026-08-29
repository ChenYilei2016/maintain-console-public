package io.github.chenyilei2016.maintain.manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableScheduling
public class ExecutionTaskConfiguration {

    @Bean
    public ThreadPoolTaskExecutor executionTaskExecutor(ManagerProperties properties) {
        ManagerProperties.Execution execution = properties.getExecution();
        return taskExecutor("maintain-task-", execution.getTaskCorePoolSize(), execution.getTaskMaxPoolSize(),
                execution.getTaskQueueCapacity());
    }

    @Bean
    public ThreadPoolTaskExecutor executionTargetExecutor(ManagerProperties properties) {
        ManagerProperties.Execution execution = properties.getExecution();
        return taskExecutor("maintain-target-", execution.getTargetCorePoolSize(), execution.getTargetMaxPoolSize(),
                execution.getTargetQueueCapacity());
    }

    private ThreadPoolTaskExecutor taskExecutor(String prefix, int coreSize, int maxSize, int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(prefix);
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }
}
