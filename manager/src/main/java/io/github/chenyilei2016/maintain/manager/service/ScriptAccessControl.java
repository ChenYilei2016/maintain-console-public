package io.github.chenyilei2016.maintain.manager.service;

import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import io.github.chenyilei2016.maintain.manager.constant.ConsoleRole;
import io.github.chenyilei2016.maintain.manager.constant.ScriptPermissionEnum;
import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;
import io.github.chenyilei2016.maintain.manager.exceptions.CommonException;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptPermissionEntity;
import io.github.chenyilei2016.maintain.manager.pojo.vo.ScriptVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 所有资源读取和操作复用当前授权；链接、收藏和历史记录不构成授权。
 */
@Component
@RequiredArgsConstructor
public class ScriptAccessControl {
    private final ScriptContentService scripts;
    private final ManagerProperties properties;

    public boolean canCreateTools(LocalLoginUser actor) {
        return properties.getGlobalWhiteEmployeeNoList().contains(actor.getEmployeeNo())
                || properties.getDeveloperEmployeeNoList().contains(actor.getEmployeeNo())
                || ConsoleRole.DEVELOPER.grantedTo(actor);
    }

    public ScriptVO require(String scriptId, String actorId, ScriptPermissionEnum permission) {
        ScriptVO script = scripts.findById(scriptId);
        if (!allows(script, actorId, permission)) {
            throw CommonException.createReminderException("没有{}权限", permission.getDesc());
        }
        return script;
    }

    public boolean allows(ScriptVO script, String actorId, ScriptPermissionEnum permission) {
        return ScriptPermissionEntity.checkPermission(script.getDirectoryNode(), script.getScript(), actorId,
                permission, properties.getGlobalWhiteEmployeeNoList());
    }

    public boolean visible(ScriptVO script, String actorId) {
        return allows(script, actorId, ScriptPermissionEnum.READ)
                || allows(script, actorId, ScriptPermissionEnum.INVOKE);
    }

    public ScriptVO requireVisible(String scriptId, String actorId) {
        ScriptVO script = scripts.findById(scriptId);
        if (!visible(script, actorId)) throw CommonException.createReminderException("无权访问此工具");
        return script;
    }
}
