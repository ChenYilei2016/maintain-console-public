package io.github.chenyilei2016.maintain.manager.pojo.mapper;

import io.github.chenyilei2016.maintain.manager.pojo.entity.DirectoryNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ToolCatalogMapper {
    /**
     * 一次有界关联读取，授权在 Module 内统一解释；不逐项加载脚本。
     */
    @Select("""
            <script>
            SELECT n.*, s.permissions AS script_permissions, s.description, s.version, s.tool_metadata,
                   p.favorite, p.last_open_time
            FROM mc_directory_node n JOIN mc_script s ON s.id = n.id
            LEFT JOIN mc_script_user_preference p ON p.script_id = n.id AND p.user_id = #{userId}
            WHERE n.type = 'script' AND n.is_deleted = 0
            <if test="serviceName != null and serviceName != ''">AND n.service_name = #{serviceName}</if>
            <if test="search != null and search != ''">AND (n.name LIKE #{search} OR s.description LIKE #{search})</if>
            <if test="view == 'MINE'">AND n.creator_id = #{userId}</if>
            <if test="view == 'SHARED'">AND n.creator_id != #{userId}</if>
            <if test="view == 'FAVORITES'">AND p.favorite = 1</if>
            <if test="view == 'RECENT'">AND p.last_open_time IS NOT NULL</if>
            ORDER BY
            <if test="view == 'RECENT'">p.last_open_time DESC,</if>
            n.update_time DESC, n.id DESC
            LIMIT 101 OFFSET #{offset}
            </script>
            """)
    List<DirectoryNode> candidates(@Param("userId") String userId, @Param("serviceName") String serviceName,
                                   @Param("search") String search, @Param("view") String view, @Param("offset") int offset);
}
