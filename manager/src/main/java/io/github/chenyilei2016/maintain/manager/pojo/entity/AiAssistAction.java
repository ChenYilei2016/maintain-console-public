package io.github.chenyilei2016.maintain.manager.pojo.entity;

public enum AiAssistAction {
    GENERATE_SCRIPT("生成 Groovy 脚本", true, false,
            "只返回可直接放入编辑器的 Groovy 代码，不要使用 Markdown 代码块。"),
    EXPLAIN_SCRIPT("解释脚本", false, true,
            "用中文解释脚本目标、主要步骤、外部影响和需要人工确认的事项。"),
    GENERATE_PARAMETER_SCHEMA("生成参数 Schema", false, true,
            "只返回合法 JSON，不要使用 Markdown 代码块。参数名必须与 $${name} 占位符完全一致。"),
    REVIEW_RISK("执行前风险审查", false, true,
            "用中文按风险等级列出写操作、远程调用、资源消耗、敏感信息和回滚检查；不得声称已执行脚本。");

    private final String displayName;
    private final boolean instructionRequired;
    private final boolean scriptRequired;
    private final String outputInstruction;

    AiAssistAction(String displayName, boolean instructionRequired, boolean scriptRequired, String outputInstruction) {
        this.displayName = displayName;
        this.instructionRequired = instructionRequired;
        this.scriptRequired = scriptRequired;
        this.outputInstruction = outputInstruction;
    }

    public String displayName() {
        return displayName;
    }

    public boolean instructionRequired() {
        return instructionRequired;
    }

    public boolean scriptRequired() {
        return scriptRequired;
    }

    public String outputInstruction() {
        return outputInstruction;
    }
}
