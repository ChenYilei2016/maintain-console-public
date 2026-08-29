package io.github.chenyilei2016.maintain.manager.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScriptFavoriteWebRequest {
    @NotBlank(message = "scriptId不能为空")
    private String scriptId;
    private boolean favorite;
}
