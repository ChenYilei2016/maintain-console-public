package io.github.chenyilei2016.maintain.manager.pojo.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 脚本执行历史数据对象
 *
 * @author chenyilei
 * @since 2025/07/31
 */
@Data
@Accessors(chain = true)
@TableName("mc_script_execution_history")
public class ScriptExecutionHistoryDO {

    /**
     * 执行记录ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private String id;

    /**
     * 脚本ID
     */
    @TableField("script_id")
    private String scriptId;

    /**
     * 脚本名称
     */
    @TableField("script_name")
    private String scriptName;

    /**
     * 执行的服务名
     */
    @TableField("service_name")
    private String serviceName;

    /**
     * 执行人ID
     */
    @TableField("executor_id")
    private String executorId;

    /**
     * 执行人姓名
     */
    @TableField("executor_name")
    private String executorName;

    /**
     * 执行时的脚本内容
     */
    @TableField("script_content")
    private String scriptContent;

    @TableField("final_script_content")
    private String finalScriptContent;

    /**
     * 执行参数JSON
     */
    @TableField("parameters")
    private String parameters;

    /**
     * 执行结果
     */
    @TableField("result")
    private String result;

    /**
     * 执行状态: success-成功, error-失败, running-运行中
     */
    @TableField("status")
    private String status;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 开始执行时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 结束执行时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 执行耗时（毫秒）
     */
    @TableField("duration")
    private Integer duration;

    /**
     * 创建时间
     */
    @TableField(value = "create_time")
    private LocalDateTime createTime;

    // 状态常量
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_ERROR = "error";
    public static final String STATUS_RUNNING = "running";
}