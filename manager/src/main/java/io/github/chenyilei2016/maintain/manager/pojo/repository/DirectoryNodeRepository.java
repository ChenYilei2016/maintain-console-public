package io.github.chenyilei2016.maintain.manager.pojo.repository;

import io.github.chenyilei2016.maintain.manager.pojo.entity.DirectoryNode;

import java.util.List;

/**
 * 目录节点仓储接口
 *
 * @author chenyilei
 * @since 2025/07/31
 */
public interface DirectoryNodeRepository {

    /**
     * 根据ID查询
     */
    DirectoryNode findById(String id);

    DirectoryNode insert(DirectoryNode directoryNode);

    /**
     * 保存或更新
     */
    DirectoryNode save(DirectoryNode directoryNode);

    /**
     * 根据父节点ID查询子节点
     */
    List<DirectoryNode> findByParentId(String parentId);


    /**
     * 逻辑删除
     */
    boolean deleteById(String id);


    /**
     * 根据服务名和创建人查询用户可见节点（所有文件夹 + 公共脚本 + 用户的私有脚本）
     */
    List<DirectoryNode> findVisibleByServiceNameAndCreator(String serviceName, String creatorId);

    List<DirectoryNode> findByNameAndParentIdAndServiceName(String name, String parentId, String serviceName);
}