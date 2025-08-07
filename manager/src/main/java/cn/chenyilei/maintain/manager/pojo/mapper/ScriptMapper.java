package cn.chenyilei.maintain.manager.pojo.mapper;

import cn.chenyilei.maintain.manager.pojo.dataobject.ScriptDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 脚本内容数据访问接口
 *
 * @author chenyilei
 * @since 2025/07/31
 */
@Mapper
public interface ScriptMapper extends BaseMapper<ScriptDO> {

    /**
     * 根据脚本ID查询脚本内容
     */
    @Select("SELECT * FROM mc_script WHERE id = #{scriptId}")
    ScriptDO selectByScriptId(@Param("scriptId") String scriptId);

    /**
     * 根据脚本ID更新版本号
     */
    @Select("UPDATE mc_script SET version = version + 1, update_time = NOW() WHERE id = #{scriptId}")
    int incrementVersion(@Param("scriptId") String scriptId);
}