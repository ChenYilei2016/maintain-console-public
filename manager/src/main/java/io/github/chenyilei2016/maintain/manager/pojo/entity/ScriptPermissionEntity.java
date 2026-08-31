package io.github.chenyilei2016.maintain.manager.pojo.entity;

import com.alibaba.fastjson2.JSON;
import io.github.chenyilei2016.maintain.manager.constant.ScriptPermissionEnum;
import io.github.chenyilei2016.maintain.manager.utils.StrUtils;
import lombok.Data;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * "description": "这里可以扩展很多权限设置, 默认只有创建人有权限编辑, invokerNo:哪些工号有权限执行, 默认创建人;
 * readerNo:哪些工号拥有权限看到脚本的代码, 默认都可以;
 * editorNo:哪些工号拥有权限编辑此脚本 , 默认创建人",
 * "invokerNo" : "12600" ,
 * "readerNo": "12600,12599",
 * "editorNo": ""
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
    @jakarta.validation.constraints.Size(max = 100)
    private List<String> allowedEnvironments;
    private boolean allowAllInstances;
    private boolean enabled = true;

    /**
     * 可阅读
     */
    @jakarta.validation.constraints.Size(max = 4096)
    private String readerNo;

    /**
     * 可编辑
     */
    @jakarta.validation.constraints.Size(max = 4096)
    private String editorNo;

    /**
     * 可执行
     */
    @jakarta.validation.constraints.Size(max = 4096)
    private String invokerNo;

    /**
     * 描述
     */
    private String description;

    public static String init(String operatorId) {
        ScriptPermissionEntity scriptPermissionEntity = new ScriptPermissionEntity();
        scriptPermissionEntity.setVersion(2);
        return JSON.toJSONString(scriptPermissionEntity);
    }

    public static ScriptPermissionEntity parse(String json) {
        ScriptPermissionEntity permission = JSON.parseObject(
                json == null || json.isBlank() ? "{}" : json, ScriptPermissionEntity.class);
        if (permission == null || (permission.version != 1 && permission.version != 2)) {
            throw new IllegalArgumentException("不支持的权限配置");
        }
        return permission;
    }

    public static boolean checkPermission(
            DirectoryNode node,
            Script existingScript,
            String operatorId,
            ScriptPermissionEnum permission,
            Set<String> globalWhiteEmployeeNos
    ) {
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
                    || grants.contains(grants.editorNo, operatorId)
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
