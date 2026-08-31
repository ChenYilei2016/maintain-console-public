package io.github.chenyilei2016.maintain.manager.pojo.mapper;

import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ScriptUserPreferenceDO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;


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

}
