package cn.chenyilei.maintain.manager.pojo.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 脚本内容数据对象
 *
 * @author chenyilei
 * @since 2025/07/31
 */
@Data
@Accessors(chain = true)
@TableName("mc_script")
public class ScriptDO {

    /**
     * 脚本ID（对应directory_node的id）
     */
    @TableId(value = "id", type = IdType.INPUT)
    private String id;

    /**
     * 脚本内容
     */
    @TableField("content")
    private String content;

    /**
     * 权限配置JSON
     */
    @TableField("permissions")
    private String permissions;

    /**
     * 脚本描述
     */
    @TableField("description")
    private String description;

    /**
     * 版本号
     */
    @TableField("version")
    private Integer version;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private LocalDateTime updateTime;
}