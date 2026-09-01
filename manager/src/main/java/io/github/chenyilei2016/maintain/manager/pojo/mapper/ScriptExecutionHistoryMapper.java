package io.github.chenyilei2016.maintain.manager.pojo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ScriptExecutionHistoryDO;
import io.github.chenyilei2016.maintain.manager.pojo.dto.UsageStatisticsDTO;
import org.apache.ibatis.annotations.*;

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

    @Select("""
            SELECT COUNT(*) AS total_executions,
                   COALESCE(SUM(CASE WHEN status = 'success' THEN 1 ELSE 0 END), 0) AS successful_executions,
                   COALESCE(SUM(CASE WHEN status = 'error' THEN 1 ELSE 0 END), 0) AS failed_executions,
                   COALESCE(AVG(duration), 0) AS average_duration_millis,
                   COUNT(DISTINCT executor_id) AS active_users,
                   COUNT(DISTINCT script_id) AS active_tools
            FROM mc_script_execution_history
            WHERE start_time >= #{since}
            """)
    @Results({
            @Result(column = "total_executions", property = "totalExecutions"),
            @Result(column = "successful_executions", property = "successfulExecutions"),
            @Result(column = "failed_executions", property = "failedExecutions"),
            @Result(column = "average_duration_millis", property = "averageDurationMillis"),
            @Result(column = "active_users", property = "activeUsers"),
            @Result(column = "active_tools", property = "activeTools")
    })
    UsageStatisticsDTO.Summary selectUsageSummary(@Param("since") LocalDateTime since);

    @Select("""
            SELECT history.script_id AS script_id,
                   COALESCE(MAX(node.name), MAX(history.script_name)) AS script_name,
                   COALESCE(MAX(node.service_name), MAX(history.service_name)) AS service_name,
                   COUNT(*) AS total_executions,
                   COALESCE(SUM(CASE WHEN history.status = 'success' THEN 1 ELSE 0 END), 0) AS successful_executions,
                   COALESCE(AVG(history.duration), 0) AS average_duration_millis,
                   MAX(history.start_time) AS last_run_time
            FROM mc_script_execution_history history
            LEFT JOIN mc_directory_node node ON node.id = history.script_id
            WHERE history.start_time >= #{since}
            GROUP BY history.script_id
            ORDER BY total_executions DESC, last_run_time DESC
            LIMIT #{limit}
            """)
    @Results({
            @Result(column = "script_id", property = "scriptId"),
            @Result(column = "script_name", property = "scriptName"),
            @Result(column = "service_name", property = "serviceName"),
            @Result(column = "total_executions", property = "totalExecutions"),
            @Result(column = "successful_executions", property = "successfulExecutions"),
            @Result(column = "average_duration_millis", property = "averageDurationMillis"),
            @Result(column = "last_run_time", property = "lastRunTime")
    })
    List<UsageStatisticsDTO.ToolUsage> selectTopToolUsage(@Param("since") LocalDateTime since,
                                                          @Param("limit") int limit);

}
