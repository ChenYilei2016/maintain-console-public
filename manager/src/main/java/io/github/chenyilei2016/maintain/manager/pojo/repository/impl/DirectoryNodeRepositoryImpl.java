package io.github.chenyilei2016.maintain.manager.pojo.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.DirectoryNodeDO;
import io.github.chenyilei2016.maintain.manager.pojo.entity.DirectoryNode;
import io.github.chenyilei2016.maintain.manager.pojo.mapper.DirectoryNodeMapper;
import io.github.chenyilei2016.maintain.manager.pojo.repository.DirectoryNodeRepository;
import io.github.chenyilei2016.maintain.manager.pojo.repository.converter.DirectoryNodeConverter;
import io.github.chenyilei2016.maintain.manager.utils.IdUtil;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * 目录节点仓储实现类
 * 作为防腐层，负责数据对象与业务实体的转换
 *
 * @author chenyilei
 * @since 2025/07/31
 */
@Repository
public class DirectoryNodeRepositoryImpl extends ServiceImpl<DirectoryNodeMapper, DirectoryNodeDO> implements DirectoryNodeRepository {

    private final DirectoryNodeConverter converter = DirectoryNodeConverter.INSTANCE;

    @Override
    public DirectoryNode findById(String id) {
        DirectoryNodeDO dataObject = getById(id);
        return converter.toEntity(dataObject);
    }


    @Override
    public DirectoryNode insert(DirectoryNode directoryNode) {
        DirectoryNodeDO dataObject = converter.toDataObject(directoryNode);
        if (dataObject.getId() == null) {
            dataObject.setId(IdUtil.generateSnowFlakeId());
            super.save(dataObject);
        } else {
            baseMapper.insert(dataObject);
        }
        return converter.toEntity(dataObject);
    }

    @Override
    public DirectoryNode save(DirectoryNode directoryNode) {
        DirectoryNodeDO dataObject = converter.toDataObject(directoryNode);
        if (dataObject.getId() == null) {
            dataObject.setId(IdUtil.generateSnowFlakeId());
            super.save(dataObject);
        } else {
            updateById(dataObject);
        }
        return converter.toEntity(dataObject);
    }

    @Override
    public List<DirectoryNode> findByParentId(String parentId) {
        List<DirectoryNodeDO> dataObjectList = baseMapper.selectByParentId(parentId);
        return converter.toEntityList(dataObjectList);
    }


    @Override
    public boolean deleteById(String id) {
        return removeById(id);
    }


    @Override
    public List<DirectoryNode> findVisibleByServiceNameAndCreator(String serviceName, String creatorId) {
        // 查询用户可见的节点：所有文件夹 + 公共脚本 + 用户的私有脚本
        LambdaQueryWrapper<DirectoryNodeDO> queryWrapper = new LambdaQueryWrapper<DirectoryNodeDO>()
                .and(wrapper -> wrapper
                        .eq(DirectoryNodeDO::getType, "folder")
                        .eq(DirectoryNodeDO::getServiceName, serviceName)
                        .eq(DirectoryNodeDO::getIsDeleted, 0)
                )// 所有文件夹
                .or(subWrapper -> subWrapper
                        .eq(DirectoryNodeDO::getType, "script")
                        .eq(DirectoryNodeDO::getPermissionType, "public") // 公共脚本
                        .eq(DirectoryNodeDO::getServiceName, serviceName)
                        .eq(DirectoryNodeDO::getIsDeleted, 0)
                )
                .or(subWrapper -> subWrapper
                        .eq(DirectoryNodeDO::getType, "script")
                        .eq(DirectoryNodeDO::getPermissionType, "private")
                        .eq(DirectoryNodeDO::getCreatorId, creatorId) // 用户的私有脚本
                        .eq(DirectoryNodeDO::getServiceName, serviceName)
                        .eq(DirectoryNodeDO::getIsDeleted, 0)
                )
                .orderByAsc(DirectoryNodeDO::getSortOrder)
                .orderByAsc(DirectoryNodeDO::getCreateTime);
        List<DirectoryNodeDO> dataObjectList = list(queryWrapper);
        return converter.toEntityList(dataObjectList);
    }

    @Override
    public List<DirectoryNode> findByNameAndParentIdAndServiceName(String name, String parentId, String serviceName) {
        LambdaQueryWrapper<DirectoryNodeDO> queryWrapper = new LambdaQueryWrapper<DirectoryNodeDO>()
                .eq(DirectoryNodeDO::getName, name)
                .eq(DirectoryNodeDO::getParentId, parentId)
                .eq(DirectoryNodeDO::getServiceName, serviceName)
                .eq(DirectoryNodeDO::getIsDeleted, 0);

        List<DirectoryNodeDO> dataObjectList = list(queryWrapper);
        if (dataObjectList == null || dataObjectList.isEmpty()) {
            return Collections.emptyList();
        }
        return converter.toEntityList(dataObjectList);
    }
}