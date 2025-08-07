package io.github.chenyilei2016.maintain.manager.constant;

import lombok.Getter;

/**
 * @author chenyilei
 * @since 2025/08/01 15:31
 */
@Getter
public enum TreeNodeTypeEnum {

    FOLDER("folder", "文件夹"),
    SCRIPT("script", "脚本");

    private final String code;

    private final String desc;

    TreeNodeTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static TreeNodeTypeEnum getEnum(String code) {
        for (TreeNodeTypeEnum type : TreeNodeTypeEnum.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    public static TreeNodeTypeEnum getEnumThrow(String code) {
        TreeNodeTypeEnum anEnum = getEnum(code);
        if (anEnum == null) {
            throw new IllegalArgumentException(String.format("No enum constant %s %s", TreeNodeTypeEnum.class, code));
        }
        return anEnum;
    }
}
