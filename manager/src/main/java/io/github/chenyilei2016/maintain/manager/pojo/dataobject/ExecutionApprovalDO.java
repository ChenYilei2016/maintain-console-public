package io.github.chenyilei2016.maintain.manager.pojo.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("mc_execution_approval")
public class ExecutionApprovalDO {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String requestDigest;
    private String scriptId;
    private String scriptName;
    private String serviceName;
    private String environment;
    private String selectionMode;
    private String requestedInstanceId;
    private String requesterId;
    private String requesterName;
    private String status;
    private String scriptContent;
    private String parameters;
    private String reason;
    private String approverId;
    private String approverName;
    private String decisionComment;
    private LocalDateTime createTime;
    private LocalDateTime expireTime;
    private LocalDateTime decisionTime;
    private LocalDateTime consumedTime;
}
