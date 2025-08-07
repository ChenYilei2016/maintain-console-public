package cn.chenyilei.maintain.manager.pojo.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 脚本内容业务实体类
 *
 * @author chenyilei
 * @since 2025/07/31
 */
@Data
@Accessors(chain = true)
public class Script {

    /**
     * 脚本ID（对应directory_node的id）
     */
    private String id;

    /**
     * 脚本内容
     */
    private String content;

    /**
     * 权限配置JSON
     */
    private String permissions;

    /**
     * 脚本描述
     */
    private String description;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 可see 可读 可写
     */
    public void checkThrowAuth(String employeeId, String employeeName) {

    }
}