package io.github.chenyilei2016.maintain.manager.constant;

import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;

/**
 * 系统角色管理平台能力；工具读、编、执仍由脚本 ACL 决定。
 */
public enum ConsoleRole {
    DEVELOPER, ADMIN, AUDITOR;

    public boolean grantedTo(LocalLoginUser actor) {
        return actor != null && actor.getRoles() != null && actor.getRoles().contains(name());
    }
}
