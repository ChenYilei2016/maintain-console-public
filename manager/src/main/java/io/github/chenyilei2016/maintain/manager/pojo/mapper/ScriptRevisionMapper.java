package io.github.chenyilei2016.maintain.manager.pojo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ScriptRevisionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ScriptRevisionMapper extends BaseMapper<ScriptRevisionDO> {

    @Select("SELECT * FROM mc_script_revision WHERE script_id = #{scriptId} ORDER BY version DESC LIMIT #{limit}")
    List<ScriptRevisionDO> selectRecent(@Param("scriptId") String scriptId, @Param("limit") int limit);

    @Select("SELECT * FROM mc_script_revision WHERE script_id = #{scriptId} AND version = #{version}")
    ScriptRevisionDO selectRevision(@Param("scriptId") String scriptId, @Param("version") int version);
}
