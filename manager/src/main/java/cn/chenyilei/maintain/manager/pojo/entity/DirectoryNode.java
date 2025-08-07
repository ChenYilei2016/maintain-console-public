package cn.chenyilei.maintain.manager.pojo.entity;

import cn.chenyilei.maintain.manager.exceptions.CommonException;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 目录节点业务实体类
 *
 * @author chenyilei
 * @since 2025/07/31
 */
@Data
@Accessors(chain = true)
public class DirectoryNode {

    /**
     * 节点ID
     */
    private String id;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点类型: folder-文件夹, script-脚本
     */
    private String type;

    /**
     * 父节点ID
     */
    private String parentId;

    /**
     * 应用服务名
     */
    private String serviceName;

    /**
     * 排序字段
     */
    private Integer sortOrder;

    /**
     * 创建人ID
     */
    private String creatorId;

    /**
     * 创建人姓名
     */
    private String creatorName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 是否删除: 0-未删除, 1-已删除
     */
    private Integer isDeleted;

    /**
     * 权限类型: public-公共, private-私有
     * 文件夹默认为public，脚本默认为public但可设置为private
     */
    private String permissionType;


    /**
     * 脚本描述（关联查询得到）
     */
    private String description;

    /**
     * 版本号（关联查询得到）
     */
    private Integer version;

    // 常量定义
    public static final String TYPE_FOLDER = "folder";
    public static final String TYPE_SCRIPT = "script";

    public static final String PERMISSION_PUBLIC = "public";
    public static final String PERMISSION_PRIVATE = "private";

    public static final String ROOT_MY_SCRIPTS = "my_scripts_root";
    public static final String ROOT_SHARED_SCRIPTS = "shared_scripts_root";

    public void checkThrowAuth(String operatorId) {
        // 检查权限
        if (Objects.equals(this.creatorId, operatorId)) {
            //有权限
            return;
        }
        throw CommonException.createBizException("无权限删除此节点: " + this.getName());
    }
}