package io.github.chenyilei2016.maintain.manager.identity;

/**
 * 认证来源是外部身份的证明方式，不代表本系统权限。
 */
public enum AuthenticationProviderType {
    LOCAL_PASSWORD,
    TRUSTED_HEADERS
}
