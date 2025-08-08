package io.github.chenyilei2016.maintain.manager.pojo.entity;

import com.alibaba.fastjson.JSON;
import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import io.github.chenyilei2016.maintain.manager.constant.ScriptPermissionEnum;
import io.github.chenyilei2016.maintain.manager.context.ApplicationContextHolder;
import io.github.chenyilei2016.maintain.manager.utils.StrUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.function.Function;

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
     * 可阅读
     */
    private String readerNo;

    /**
     * 可编辑
     */
    private String editorNo;

    /**
     * 可执行
     */
    private String invokerNo;

    /**
     * 描述
     */
    private String description;

    public static String init(String operatorId) {
        ScriptPermissionEntity scriptPermissionEntity = new ScriptPermissionEntity();
        return JSON.toJSONString(scriptPermissionEntity);
    }

    public static boolean checkPermission(DirectoryNode node, Script existingScript, String operatorId, ScriptPermissionEnum p) {
        ManagerProperties mp = ApplicationContextHolder.getApplicationContext().getBean(ManagerProperties.class);
        if (mp.getGlobalWhiteEmployeeNoList().contains(operatorId)) {
            return true;
        }
        if (Objects.equals(node.getCreatorId(), operatorId)) {
            return true;
        }
        return checkPermission(existingScript, operatorId, p);
    }

    public static boolean checkPermission(Script existingScript, String operatorId, ScriptPermissionEnum p) {
        String permissions = existingScript.getPermissions();
        if (StringUtils.isBlank(permissions)) {
            permissions = "{}";
        }
        ScriptPermissionEntity scriptPermissionEntity = JSON.parseObject(permissions, ScriptPermissionEntity.class);

        boolean isAuth = false;
        switch (p) {
            case INVOKE:
                isAuth = scriptPermissionEntity.checkAuth(scriptPermissionEntity, ScriptPermissionEntity::getInvokerNo, operatorId);
                break;
            case EDIT:
                isAuth = scriptPermissionEntity.checkAuth(scriptPermissionEntity, ScriptPermissionEntity::getEditorNo, operatorId);
                break;
            case READ:
                //默认都有
                if (StringUtils.isBlank(scriptPermissionEntity.getReaderNo())) {
                    isAuth = true;
                } else {
                    isAuth = scriptPermissionEntity.checkAuth(scriptPermissionEntity, ScriptPermissionEntity::getReaderNo, operatorId);
                }
                break;
        }
        return isAuth;
    }

    private boolean checkAuth(ScriptPermissionEntity entity, Function<ScriptPermissionEntity, String> getInvokerNo, String operatorId) {
        String apply = getInvokerNo.apply(entity);
        if (apply == null) {
            apply = "";
        }
        return StrUtils.commaSplitter.splitToList(apply).contains(operatorId);
    }


}
