package io.github.chenyilei2016.maintain.manager.pojo.dataobject;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("mc_script_user_preference")
public class ScriptUserPreferenceDO {
    private String userId;
    private String scriptId;
    private Boolean favorite;
    private LocalDateTime lastOpenTime;
    private Integer openCount;
    private LocalDateTime updateTime;
}
