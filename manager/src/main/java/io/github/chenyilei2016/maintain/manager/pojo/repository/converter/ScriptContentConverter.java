package io.github.chenyilei2016.maintain.manager.pojo.repository.converter;

import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ScriptDO;
import io.github.chenyilei2016.maintain.manager.pojo.entity.Script;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 脚本内容转换器
 *
 * @author chenyilei
 * @since 2025/07/31
 */
@Mapper
public interface ScriptContentConverter {

    ScriptContentConverter INSTANCE = Mappers.getMapper(ScriptContentConverter.class);

    /**
     * DataObject 转 Entity
     */
    Script toEntity(ScriptDO dataObject);

    /**
     * Entity 转 DataObject
     */
    ScriptDO toDataObject(Script entity);

    /**
     * DataObject 列表转 Entity 列表
     */
    List<Script> toEntityList(List<ScriptDO> dataObjectList);

    /**
     * Entity 列表转 DataObject 列表
     */
    List<ScriptDO> toDataObjectList(List<Script> entityList);
}