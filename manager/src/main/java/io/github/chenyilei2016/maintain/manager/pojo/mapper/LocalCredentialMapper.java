package io.github.chenyilei2016.maintain.manager.pojo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.LocalCredentialDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LocalCredentialMapper extends BaseMapper<LocalCredentialDO> {
    @Select("SELECT * FROM mc_local_credential WHERE username = #{username}")
    LocalCredentialDO selectByUsername(@Param("username") String username);

    @Select("""
            SELECT COUNT(*)
            FROM mc_local_credential credential
            JOIN mc_console_user console_user ON console_user.id = credential.user_id
            WHERE console_user.status = 'ACTIVE' AND console_user.roles LIKE '%\"ADMIN\"%'
            """)
    int countActiveAdministrators();
}
