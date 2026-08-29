package io.github.chenyilei2016.maintain.manager.pojo.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScriptShortcutDTO {
    private String id;
    private String name;
    private String serviceName;
    private String parentId;
    private boolean favorite;
    private LocalDateTime lastOpenTime;
}
