package io.github.chenyilei2016.maintain.manager.identity;

public enum ConsoleUserStatus {
    ACTIVE,
    DISABLED;

    public boolean canLogin() {
        return this == ACTIVE;
    }
}
