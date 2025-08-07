package cn.chenyilei.maintain.manager.pojo.mapper;

import cn.chenyilei.maintain.manager.pojo.dataobject.ScriptExecutionHistoryDO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 脚本执行历史数据访问接口
 *
 * @author chenyilei
 * @since 2025/07/31
 */
@Mapper
public interface ScriptExecutionHistoryMapper extends BaseMapper<ScriptExecutionHistoryDO> {

    /**
     * 根据脚本ID查询执行历史
     */
    @Select("SELECT * FROM mc_script_execution_history WHERE script_id = #{scriptId} ORDER BY create_time DESC")
    List<ScriptExecutionHistoryDO> selectByScriptId(@Param("scriptId") String scriptId);

    /**
     * 根据执行人查询执行历史
     */
    @Select("SELECT * FROM mc_script_execution_history WHERE executor_name = #{executorName} ORDER BY create_time DESC LIMIT #{limit}")
    List<ScriptExecutionHistoryDO> selectByExecutor(@Param("executorName") String executorName, @Param("limit") int limit);

    /**
     * 根据状态查询执行历史
     */
    @Select("SELECT * FROM mc_script_execution_history WHERE status = #{status} ORDER BY create_time DESC")
    List<ScriptExecutionHistoryDO> selectByStatus(@Param("status") String status);

    /**
     * 根据时间范围查询执行历史
     */
    @Select("SELECT * FROM mc_script_execution_history WHERE create_time BETWEEN #{startTime} AND #{endTime} ORDER BY create_time DESC")
    List<ScriptExecutionHistoryDO> selectByTimeRange(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 统计执行次数
     */
    @Select("SELECT COUNT(*) FROM mc_script_execution_history WHERE script_id = #{scriptId}")
    int countByScriptId(@Param("scriptId") String scriptId);

}