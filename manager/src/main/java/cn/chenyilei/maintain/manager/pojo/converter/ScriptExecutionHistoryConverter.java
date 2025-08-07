package cn.chenyilei.maintain.manager.pojo.converter;

import cn.chenyilei.maintain.manager.pojo.dataobject.ScriptExecutionHistoryDO;
import cn.chenyilei.maintain.manager.pojo.entity.ScriptExecutionHistoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * 脚本执行历史转换器
 *
 * @author chenyilei
 * @since 2025/08/05
 */
@Mapper
public interface ScriptExecutionHistoryConverter {

    ScriptExecutionHistoryConverter INSTANCE = Mappers.getMapper(ScriptExecutionHistoryConverter.class);

    ScriptExecutionHistoryDO toDataObject(ScriptExecutionHistoryEntity entity);

    ScriptExecutionHistoryEntity toEntity(ScriptExecutionHistoryDO dataObject);
}
