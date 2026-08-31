package io.github.chenyilei2016.maintain.manager.constant;

import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;

/**
 * 复用可信身份中的角色声明，不建设独立角色管理系统。
 */
public enum ConsoleRole {
    DEVELOPER, ADMIN, AUDITOR;

    public boolean grantedTo(LocalLoginUser actor) {
        return actor.getRoles().contains(name());
    }
}
