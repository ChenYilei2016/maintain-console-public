package io.github.chenyilei2016.maintain.manager.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public record UsageStatisticsDTO(String window, int days, Summary summary, List<ToolUsage> tools) {
    @Data
    public static class Summary {
        private long totalExecutions;
        private long successfulExecutions;
        private long failedExecutions;
        private double averageDurationMillis;
        private long activeUsers;
        private long activeTools;
    }

    @Data
    public static class ToolUsage {
        private String scriptId;
        private String scriptName;
        private String serviceName;
        private long totalExecutions;
        private long successfulExecutions;
        private double averageDurationMillis;
        private LocalDateTime lastRunTime;
    }
}
