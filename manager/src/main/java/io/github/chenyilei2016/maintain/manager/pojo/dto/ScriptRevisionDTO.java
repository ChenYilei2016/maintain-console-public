package io.github.chenyilei2016.maintain.manager.pojo.dto;

import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptRevision;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.time.LocalDateTime;

@Data
public class ScriptRevisionDTO {
    private String id;
    private String scriptId;
    private Integer version;
    private String content;
    private String parameterSchema;
    private String description;
    private String creatorId;
    private String creatorName;
    private LocalDateTime createTime;

    public static ScriptRevisionDTO from(ScriptRevision revision) {
        ScriptRevisionDTO dto = new ScriptRevisionDTO();
        BeanUtils.copyProperties(revision, dto);
        return dto;
    }
}
