package io.github.chenyilei2016.maintain.manager.pojo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ConsoleUserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ConsoleUserMapper extends BaseMapper<ConsoleUserDO> {
    @Select("SELECT * FROM mc_console_user WHERE employee_no = #{employeeNo}")
    ConsoleUserDO selectByEmployeeNo(@Param("employeeNo") String employeeNo);

    @Update("""
            UPDATE mc_console_user SET update_time = update_time
            WHERE id = (SELECT id FROM (SELECT id FROM mc_console_user ORDER BY id LIMIT 1) user_lock)
            """)
    int lockUserAdministration();

    @Select("SELECT COUNT(*) FROM mc_console_user WHERE status = 'ACTIVE' AND roles LIKE '%\"ADMIN\"%'")
    int countActiveAdministrators();
}
