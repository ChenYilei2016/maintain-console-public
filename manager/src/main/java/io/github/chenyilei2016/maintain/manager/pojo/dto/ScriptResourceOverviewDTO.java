package io.github.chenyilei2016.maintain.manager.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class ScriptResourceOverviewDTO {
    private List<ScriptShortcutDTO> favorites;
    private List<ScriptShortcutDTO> recent;
}
