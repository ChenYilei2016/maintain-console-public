package cn.chenyilei.maintain.manager.pojo.repository;

import cn.chenyilei.maintain.manager.pojo.dataobject.ScriptExecutionHistoryDO;
import cn.chenyilei.maintain.manager.pojo.entity.ScriptExecutionHistoryEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 脚本执行历史 仓库类
 *
 * @author chenyilei
 * @since 2025/08/05
 */
public interface ScriptExecutionHistoryRepository {

    //todo: 暂时省时间..
    IPage<ScriptExecutionHistoryEntity> page(IPage page, QueryWrapper<ScriptExecutionHistoryDO> queryWrapper);

    boolean save(ScriptExecutionHistoryEntity historyEntity);

}
