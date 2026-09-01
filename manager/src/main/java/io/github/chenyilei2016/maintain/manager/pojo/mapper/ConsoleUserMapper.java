package io.github.chenyilei2016.maintain.manager.pojo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ConsoleUserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ConsoleUserMapper extends BaseMapper<ConsoleUserDO> {
    @Select("SELECT * FROM mc_console_user WHERE provider = #{provider} AND external_subject = #{subject}")
    ConsoleUserDO selectByExternalIdentity(@Param("provider") String provider, @Param("subject") String subject);
}
