package io.github.chenyilei2016.maintain.manager.pojo.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("mc_console_user")
public class ConsoleUserDO {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    private String provider;
    @TableField("external_subject")
    private String externalSubject;
    @TableField("employee_no")
    private String employeeNo;
    @TableField("display_name")
    private String displayName;
    private String roles;
    private String status;
    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
