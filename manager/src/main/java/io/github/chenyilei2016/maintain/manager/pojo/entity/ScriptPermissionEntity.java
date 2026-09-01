package io.github.chenyilei2016.maintain.manager.pojo.entity;

import com.alibaba.fastjson2.JSON;
import io.github.chenyilei2016.maintain.manager.constant.ConsoleRole;
import io.github.chenyilei2016.maintain.manager.constant.ScriptPermissionEnum;
import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;
import io.github.chenyilei2016.maintain.manager.utils.StrUtils;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 资源授权与执行范围。v1 保留旧公开读取语义，v2 空名单为私有；创建人及全局管理员隐式拥有管理权限。
 *
 * @author chenyilei
 * @since 2025/08/06 16:07
 */
@Data
public class ScriptPermissionEntity {
    /**
     * 旧配置缺省为 1；新配置显式授权，空名单不再代表公开。
     */
    private int version = 1;
    @Size(max = 100)
    private List<String> allowedEnvironments;
    private boolean allowAllInstances;
    private boolean enabled = true;

    /**
     * 可阅读
     */
    @Size(max = 4096)
    private String readerNo;

    /**
     * 可编辑
     */
    @Size(max = 4096)
    private String editorNo;

    /**
     * 可执行
     */
    @Size(max = 4096)
    private String invokerNo;

    /**
     * 描述
     */
    private String description;

    public static ScriptPermissionEntity privateTool() {
        ScriptPermissionEntity permissions = new ScriptPermissionEntity();
        permissions.setVersion(2);
        return permissions;
    }

    public static ScriptPermissionEntity parse(String json) {
        ScriptPermissionEntity permission = JSON.parseObject(
                json == null || json.isBlank() ? "{}" : json, ScriptPermissionEntity.class);
        if (permission == null || (permission.version != 1 && permission.version != 2)) {
            throw new IllegalArgumentException("不支持的权限配置");
        }
        return permission;
    }

    public static boolean checkPermission(DirectoryNode node, Script existingScript, LocalLoginUser actor,
                                          ScriptPermissionEnum permission, Set<String> globalWhiteEmployeeNos) {
        if (ConsoleRole.ADMIN.grantedTo(actor)) return true;
        String operatorId = actor.getEmployeeNo();
        if (globalWhiteEmployeeNos.contains(operatorId)) {
            return true;
        }
        if (Objects.equals(node.getCreatorId(), operatorId)) {
            return true;
        }
        ScriptPermissionEntity grants = parse(existingScript.getPermissions());
        return switch (permission) {
            case MANAGE -> false;
            case INVOKE -> grants.contains(grants.invokerNo, operatorId);
            case EDIT -> grants.contains(grants.editorNo, operatorId);
            case READ -> grants.contains(grants.readerNo, operatorId)
                    || (grants.version == 2 && grants.contains(grants.editorNo, operatorId))
                    || (grants.version == 1 && DirectoryNode.PERMISSION_PUBLIC.equals(node.getPermissionType())
                    && (grants.readerNo == null || grants.readerNo.isBlank()));
        };
    }

    private boolean contains(String employees, String operatorId) {
        return employees != null && StrUtils.commaSplitter.splitToList(employees).contains(operatorId);
    }

    public boolean allowsEnvironment(String environment, boolean legacyDevelopment) {
        return enabled && (allowedEnvironments == null
                ? version == 1 && legacyDevelopment : allowedEnvironments.contains(environment));
    }


}
