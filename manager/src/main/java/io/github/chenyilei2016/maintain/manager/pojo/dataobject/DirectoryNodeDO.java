package io.github.chenyilei2016.maintain.manager.pojo.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 目录节点数据对象
 *
 * @author chenyilei
 * @since 2025/07/31
 */
@Data
@Accessors(chain = true)
@TableName("mc_directory_node")
public class DirectoryNodeDO {

    /**
     * 节点ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 节点名称
     */
    @TableField("name")
    private String name;

    /**
     * 节点类型: folder-文件夹, script-脚本
     */
    @TableField("type")
    private String type;

    /**
     * 父节点ID
     */
    @TableField("parent_id")
    private String parentId;

    /**
     * 应用服务名
     */
    @TableField("service_name")
    private String serviceName;

    /**
     * 排序字段
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 创建人 ID
     */
    @TableField("creator_id")
    private String creatorId;

    /**
     * 创建人姓名
     */
    @TableField("creator_name")
    private String creatorName;

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

    /**
     * 是否删除: 0-未删除, 1-已删除
     */
    @TableField("is_deleted")
    @TableLogic
    private Integer isDeleted;

    /**
     * 权限类型: public-公共, private-私有
     * 文件夹默认为public，脚本默认为public但可设置为private
     */
    @TableField("permission_type")
    private String permissionType;

    // 常量定义
    public static final String TYPE_FOLDER = "folder";
    public static final String TYPE_SCRIPT = "script";

    public static final String PERMISSION_PUBLIC = "public";
    public static final String PERMISSION_PRIVATE = "private";

    public static final String ROOT_MY_SCRIPTS = "my_scripts_root";
    public static final String ROOT_SHARED_SCRIPTS = "shared_scripts_root";
}