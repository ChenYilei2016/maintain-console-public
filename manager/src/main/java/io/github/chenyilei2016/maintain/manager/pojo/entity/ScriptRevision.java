package io.github.chenyilei2016.maintain.manager.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScriptRevision {
    private String id;
    private String scriptId;
    private Integer version;
    private String content;
    private String parameterSchema;
    private String permissions;
    private String description;
    private String creatorId;
    private String creatorName;
    private LocalDateTime createTime;
}
