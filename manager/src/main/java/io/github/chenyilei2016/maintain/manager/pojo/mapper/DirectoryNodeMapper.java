package io.github.chenyilei2016.maintain.manager.pojo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.DirectoryNodeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 目录节点数据访问接口
 *
 * @author chenyilei
 * @since 2025/07/31
 */
@Mapper
public interface DirectoryNodeMapper extends BaseMapper<DirectoryNodeDO> {

    /**
     * 根据父节点ID查询子节点
     */
    @Select("SELECT * FROM mc_directory_node WHERE parent_id = #{parentId} AND is_deleted = 0 ORDER BY sort_order ASC, create_time ASC")
    List<DirectoryNodeDO> selectByParentId(@Param("parentId") String parentId);

    /**
     * 根据服务名查询根节点
     */
    @Select("SELECT * FROM mc_directory_node WHERE service_name = #{serviceName} AND parent_id IS NULL AND is_deleted = 0 ORDER BY sort_order ASC")
    List<DirectoryNodeDO> selectRootNodesByServiceName(@Param("serviceName") String serviceName);

    /**
     * 根据创建人查询节点
     */
    @Select("SELECT * FROM mc_directory_node WHERE creator = #{creator} AND is_deleted = 0 ORDER BY create_time DESC")
    List<DirectoryNodeDO> selectByCreator(@Param("creator") String creator);

    /**
     * 根据类型查询节点
     */
    @Select("SELECT * FROM mc_directory_node WHERE type = #{type} AND is_deleted = 0 ORDER BY create_time DESC")
    List<DirectoryNodeDO> selectByType(@Param("type") String type);

    /**
     * 统计子节点数量
     */
    @Select("SELECT COUNT(*) FROM mc_directory_node WHERE parent_id = #{parentId} AND is_deleted = 0")
    int countChildrenByParentId(@Param("parentId") String parentId);
}