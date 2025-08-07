package cn.chenyilei.maintain.manager.pojo.repository.converter;

import cn.chenyilei.maintain.manager.pojo.dataobject.DirectoryNodeDO;
import cn.chenyilei.maintain.manager.pojo.entity.DirectoryNode;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 目录节点转换器
 *
 * @author chenyilei
 * @since 2025/07/31
 */
@Mapper
public interface DirectoryNodeConverter {

    DirectoryNodeConverter INSTANCE = Mappers.getMapper(DirectoryNodeConverter.class);

    /**
     * DataObject 转 Entity
     */
    DirectoryNode toEntity(DirectoryNodeDO dataObject);

    /**
     * Entity 转 DataObject
     */
    DirectoryNodeDO toDataObject(DirectoryNode entity);

    /**
     * DataObject 列表转 Entity 列表
     */
    List<DirectoryNode> toEntityList(List<DirectoryNodeDO> dataObjectList);

    /**
     * Entity 列表转 DataObject 列表
     */
    List<DirectoryNodeDO> toDataObjectList(List<DirectoryNode> entityList);
}