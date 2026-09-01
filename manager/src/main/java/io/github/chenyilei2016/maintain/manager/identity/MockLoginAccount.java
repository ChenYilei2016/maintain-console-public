package io.github.chenyilei2016.maintain.manager.identity;

import io.github.chenyilei2016.maintain.manager.constant.ConsoleRole;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * local/demo 唯一允许选择的固定身份。普通使用者没有系统角色，由脚本 ACL 单独授权。
 */
public enum MockLoginAccount {
    ADMIN("admin", "演示管理员", "管理用户、环境，并可制作工具", Set.of(ConsoleRole.ADMIN, ConsoleRole.DEVELOPER, ConsoleRole.AUDITOR)),
    DEVELOPER("developer", "演示开发者", "创建、编辑和调试工具", Set.of(ConsoleRole.DEVELOPER)),
    OPERATOR("operator", "演示使用者", "只使用明确授权的工具", Set.of());

    private final String accountId;
    private final String displayName;
    private final String description;
    private final Set<ConsoleRole> initialRoles;

    MockLoginAccount(String accountId, String displayName, String description, Set<ConsoleRole> initialRoles) {
        this.accountId = accountId;
        this.displayName = displayName;
        this.description = description;
        this.initialRoles = initialRoles;
    }

    public ExternalIdentity verifiedIdentity() {
        return new ExternalIdentity(AuthenticationProviderType.MOCK_SDK, accountId, accountId, displayName, initialRoles);
    }

    public Option option() {
        return new Option(accountId, displayName, description);
    }

    public static MockLoginAccount require(String accountId) {
        return Arrays.stream(values()).filter(account -> account.accountId.equals(accountId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的演示账号"));
    }

    public static List<Option> options() {
        return Arrays.stream(values()).map(MockLoginAccount::option).toList();
    }

    public record Option(String id, String name, String description) {
    }
}
