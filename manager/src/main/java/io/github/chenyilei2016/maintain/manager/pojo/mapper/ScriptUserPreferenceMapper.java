package io.github.chenyilei2016.maintain.manager.pojo.mapper;

import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ScriptUserPreferenceDO;
import io.github.chenyilei2016.maintain.manager.pojo.dto.ScriptShortcutDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ScriptUserPreferenceMapper {
    @Update("""
            UPDATE mc_script_user_preference
            SET last_open_time = #{lastOpenTime}, update_time = #{updateTime}, open_count = open_count + 1
            WHERE user_id = #{userId} AND script_id = #{scriptId}
            """)
    int touch(ScriptUserPreferenceDO preference);

    @Update("""
            UPDATE mc_script_user_preference
            SET favorite = #{favorite}, update_time = #{updateTime}
            WHERE user_id = #{userId} AND script_id = #{scriptId}
            """)
    int updateFavorite(ScriptUserPreferenceDO preference);

    @Insert("""
            INSERT INTO mc_script_user_preference (
                user_id, script_id, favorite, last_open_time, open_count, update_time
            ) VALUES (
                #{userId}, #{scriptId}, #{favorite}, #{lastOpenTime}, #{openCount}, #{updateTime}
            )
            """)
    int insert(ScriptUserPreferenceDO preference);

    @Select("""
            SELECT n.id, n.name, n.service_name, n.parent_id,
                   p.favorite, p.last_open_time
            FROM mc_script_user_preference p
            JOIN mc_directory_node n ON n.id = p.script_id
            WHERE p.user_id = #{userId} AND n.service_name = #{serviceName}
              AND p.favorite = 1 AND n.is_deleted = 0
              AND (n.creator_id = #{userId} OR n.permission_type = 'public')
            ORDER BY p.update_time DESC
            LIMIT 20
            """)
    List<ScriptShortcutDTO> selectFavorites(@Param("userId") String userId, @Param("serviceName") String serviceName);

    @Select("""
            SELECT n.id, n.name, n.service_name, n.parent_id,
                   p.favorite, p.last_open_time
            FROM mc_script_user_preference p
            JOIN mc_directory_node n ON n.id = p.script_id
            WHERE p.user_id = #{userId} AND n.service_name = #{serviceName}
              AND p.last_open_time IS NOT NULL AND n.is_deleted = 0
              AND (n.creator_id = #{userId} OR n.permission_type = 'public')
            ORDER BY p.last_open_time DESC
            LIMIT 20
            """)
    List<ScriptShortcutDTO> selectRecent(@Param("userId") String userId, @Param("serviceName") String serviceName);
}
