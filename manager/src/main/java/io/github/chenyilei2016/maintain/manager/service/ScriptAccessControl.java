package io.github.chenyilei2016.maintain.manager.service;

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

    public ScriptVO require(String scriptId, LocalLoginUser actor, ScriptPermissionEnum permission) {
        ScriptVO script = scripts.findById(scriptId);
        if (!allows(script, actor, permission)) {
            throw CommonException.createReminderException("没有{}权限", permission.getDesc());
        }
        return script;
    }

    public ScriptVO requireAny(String scriptId, LocalLoginUser actor, ScriptPermissionEnum... permissions) {
        ScriptVO script = scripts.findById(scriptId);
        if (java.util.Arrays.stream(permissions).noneMatch(permission -> allows(script, actor, permission))) {
            throw CommonException.createReminderException("没有所需的脚本权限");
        }
        return script;
    }

    public boolean allows(ScriptVO script, LocalLoginUser actor, ScriptPermissionEnum permission) {
        return ScriptPermissionEntity.checkPermission(script.getDirectoryNode(), script.getScript(), actor, permission);
    }

    public boolean visible(ScriptVO script, LocalLoginUser actor) {
        return allows(script, actor, ScriptPermissionEnum.READ) || allows(script, actor, ScriptPermissionEnum.INVOKE);
    }

    public ScriptVO requireVisible(String scriptId, LocalLoginUser actor) {
        ScriptVO script = scripts.findById(scriptId);
        if (!visible(script, actor)) throw CommonException.createReminderException("无权访问此工具");
        return script;
    }

}
