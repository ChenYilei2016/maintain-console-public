package io.github.chenyilei2016.maintain.manager.identity;

import io.github.chenyilei2016.maintain.manager.constant.ConsoleRole;

import java.util.Set;

/**
 * 已由认证来源验证的身份；前端提交的账号名不能直接构造此对象。
 */
public record ExternalIdentity(AuthenticationProviderType provider, String subject, String employeeNo,
                               String displayName, Set<ConsoleRole> initialRoles) {
}
