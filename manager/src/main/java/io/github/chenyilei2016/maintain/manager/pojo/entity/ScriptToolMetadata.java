package io.github.chenyilei2016.maintain.manager.pojo.entity;

import com.alibaba.fastjson2.JSON;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ScriptToolMetadata {
    @jakarta.validation.constraints.NotNull
    private OperationType operationType = OperationType.UNSPECIFIED;
    @Size(max = 4000)
    private String riskNote;
    @Size(max = 4000)
    private String usageExample;

    public static ScriptToolMetadata parse(String json) {
        return json == null || json.isBlank() ? new ScriptToolMetadata()
                : JSON.parseObject(json, ScriptToolMetadata.class);
    }

    /**
     * 用途标识，不代表 Groovy 只读约束。
     */
    public enum OperationType {
        UNSPECIFIED, QUERY, OPERATION;

        public boolean requiresConfirmation() {
            return this != QUERY;
        }
    }
}
