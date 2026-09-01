package io.github.chenyilei2016.maintain.manager.pojo.repository;

import io.github.chenyilei2016.maintain.manager.pojo.entity.DirectoryNode;

import java.time.LocalDateTime;
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

    boolean deleteAll(List<String> ids);


    /**
     * 有界读取服务资源树及授权元数据；调用者必须按实际操作校验权限。
     */
    List<DirectoryNode> findServiceTree(String serviceName);

    List<DirectoryNode> findByNameAndParentIdAndServiceName(String name, String parentId, String serviceName);

    boolean updateParentId(String id, String parentId, LocalDateTime updateTime);
}
