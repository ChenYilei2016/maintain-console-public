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

import java.time.LocalDateTime;
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
    private static final int MAX_TREE_NODES = 500;

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
    public boolean deleteAll(List<String> ids) {
        return !ids.isEmpty() && baseMapper.deleteByIds(ids) == ids.size();
    }


    @Override
    public List<DirectoryNode> findServiceTree(String serviceName) {
        List<DirectoryNode> nodes = baseMapper.selectServiceTree(serviceName, MAX_TREE_NODES + 1);
        if (nodes.size() > MAX_TREE_NODES) {
            throw io.github.chenyilei2016.maintain.manager.exceptions.CommonException.createReminderException(
                    "资源树超过 500 个节点，需要启用服务端树分页后继续查找");
        }
        return nodes;
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

    @Override
    public boolean updateParentId(String id, String parentId, LocalDateTime updateTime) {
        return baseMapper.updateParentId(id, parentId, updateTime) == 1;
    }
}
