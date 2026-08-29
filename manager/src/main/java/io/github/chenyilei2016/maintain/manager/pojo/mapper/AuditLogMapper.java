package io.github.chenyilei2016.maintain.manager.pojo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.AuditLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLogDO> {
}
