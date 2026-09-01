package io.github.chenyilei2016.maintain.manager.constant;

import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;

/**
 * 系统角色只管理平台能力；脚本读、编、执、管仍由脚本 JSON 决定。
 */
public enum ConsoleRole {
    ADMIN;

    public boolean grantedTo(LocalLoginUser actor) {
        return actor != null && actor.getRoles() != null && actor.getRoles().contains(name());
    }
}
