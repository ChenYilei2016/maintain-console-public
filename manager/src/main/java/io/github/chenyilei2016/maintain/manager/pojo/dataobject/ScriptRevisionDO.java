package io.github.chenyilei2016.maintain.manager.pojo.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mc_script_revision")
public class ScriptRevisionDO {
    @TableId(value = "id", type = IdType.INPUT)
    private String id;
    @TableField("script_id")
    private String scriptId;
    private Integer version;
    private String content;
    @TableField("parameter_schema")
    private String parameterSchema;
    private String permissions;
    private String description;
    @TableField("creator_id")
    private String creatorId;
    @TableField("creator_name")
    private String creatorName;
    @TableField("create_time")
    private LocalDateTime createTime;
}
