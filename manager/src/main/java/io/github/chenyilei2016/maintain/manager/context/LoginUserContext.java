package io.github.chenyilei2016.maintain.manager.context;

/**
 * @author chenyilei
 * @since 2025/07/30 20:00
 */
public class LoginUserContext {
    private static final ThreadLocal<LocalLoginUser> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * todo: 权限修改点
     *
     * @return
     */
    public static LocalLoginUser getUser() {

        return LocalLoginUser.mock();
//        return THREAD_LOCAL.get();
    }

    public static void setUser(LocalLoginUser shop) {
        THREAD_LOCAL.set(shop);
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }
}
