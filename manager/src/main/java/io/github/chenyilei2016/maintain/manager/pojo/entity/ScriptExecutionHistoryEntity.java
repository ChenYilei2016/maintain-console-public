package io.github.chenyilei2016.maintain.manager.pojo.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 脚本执行历史实体
 *
 * @author chenyilei
 * @since 2025/08/05
 */
@Data
@Accessors(chain = true)
public class ScriptExecutionHistoryEntity {

    private String id;
    private String scriptId;
    private String scriptName;
    private String serviceName;
    private String executorId;
    private String executorName;
    private String scriptContent;
    private String finalScriptContent;
    private String parameters;
    private String result;
    private String status;
    private String errorMessage;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private LocalDateTime createTime;
}
