package cn.chenyilei.maintain.manager.constant;

import lombok.Getter;

/**
 * @author chenyilei
 * @since 2025/08/07 14:28
 */
@Getter
public enum ScriptPermissionEnum {

    READ("READ", "读"),
    EDIT("EDIT", "编辑"),
    INVOKE("INVOKE", "执行");

    private final String code;

    private final String desc;

    ScriptPermissionEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ScriptPermissionEnum getEnum(String code) {
        for (ScriptPermissionEnum type : ScriptPermissionEnum.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static ScriptPermissionEnum getEnumThrow(String code) {
        ScriptPermissionEnum anEnum = getEnum(code);
        if (anEnum == null) {
            throw new IllegalArgumentException(String.format("No enum constant %s %s", ScriptPermissionEnum.class, code));
        }
        return anEnum;
    }
}