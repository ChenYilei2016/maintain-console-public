package io.github.chenyilei2016.maintain.manager.pojo.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("mc_script_execution_task")
public class ScriptExecutionTaskDO {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String scriptId;
    private String scriptName;
    private String serviceName;
    private String environment;
    private String selectionMode;
    private String requestedInstanceId;
    private String executorId;
    private String executorName;
    private String scriptContent;
    private String finalScriptContent;
    private String parameters;
    private String status;
    private String targetsJson;
    private Integer timeoutSeconds;
    private Boolean cancelRequested;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private String approvalId;
    private Boolean production;
}
