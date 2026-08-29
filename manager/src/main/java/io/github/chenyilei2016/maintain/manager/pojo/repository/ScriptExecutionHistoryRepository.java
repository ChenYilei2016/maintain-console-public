package io.github.chenyilei2016.maintain.manager.pojo.repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ScriptExecutionHistoryDO;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptExecutionHistoryEntity;

/**
 * 脚本执行历史 仓库类
 *
 * @author chenyilei
 * @since 2025/08/05
 */
public interface ScriptExecutionHistoryRepository {

    IPage<ScriptExecutionHistoryEntity> page(
            Page<ScriptExecutionHistoryEntity> page,
            QueryWrapper<ScriptExecutionHistoryDO> queryWrapper
    );

    boolean save(ScriptExecutionHistoryEntity historyEntity);

}
