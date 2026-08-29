package io.github.chenyilei2016.maintain.manager.pojo.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("mc_audit_log")
public class AuditLogDO {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String actorId;
    private String actorName;
    private String action;
    private String targetType;
    private String targetId;
    private String outcome;
    private String details;
    private String clientIp;
    private String userAgent;
    private LocalDateTime createTime;
}
