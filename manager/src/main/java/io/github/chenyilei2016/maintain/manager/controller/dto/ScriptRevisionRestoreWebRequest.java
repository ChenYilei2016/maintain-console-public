package io.github.chenyilei2016.maintain.manager.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScriptRevisionRestoreWebRequest {
    @NotBlank(message = "脚本 ID 不能为空")
    private String scriptId;

    @Min(value = 1, message = "版本号必须大于 0")
    private int version;
}
