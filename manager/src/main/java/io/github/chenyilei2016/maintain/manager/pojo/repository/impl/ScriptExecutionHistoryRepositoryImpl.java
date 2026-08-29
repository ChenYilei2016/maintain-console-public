package io.github.chenyilei2016.maintain.manager.pojo.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.chenyilei2016.maintain.manager.pojo.converter.ScriptExecutionHistoryConverter;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ScriptExecutionHistoryDO;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptExecutionHistoryEntity;
import io.github.chenyilei2016.maintain.manager.pojo.mapper.ScriptExecutionHistoryMapper;
import io.github.chenyilei2016.maintain.manager.pojo.repository.ScriptExecutionHistoryRepository;
import org.springframework.stereotype.Repository;

/**
 * 脚本执行历史 仓库实现类
 *
 * @author chenyilei
 * @since 2025/08/05
 */
@Repository
public class ScriptExecutionHistoryRepositoryImpl extends ServiceImpl<ScriptExecutionHistoryMapper, ScriptExecutionHistoryDO> implements ScriptExecutionHistoryRepository {

    @Override
    public IPage<ScriptExecutionHistoryEntity> page(
            Page<ScriptExecutionHistoryEntity> page,
            QueryWrapper<ScriptExecutionHistoryDO> queryWrapper
    ) {
        Page<ScriptExecutionHistoryDO> dataPage = baseMapper.selectPage(
                new Page<>(page.getCurrent(), page.getSize()), queryWrapper);
        page.setRecords(dataPage.getRecords().stream()
                .map(ScriptExecutionHistoryConverter.INSTANCE::toEntity)
                .toList());
        page.setTotal(dataPage.getTotal());
        return page;
    }

    public boolean save(ScriptExecutionHistoryEntity entity) {
        ScriptExecutionHistoryDO dataObject = ScriptExecutionHistoryConverter.INSTANCE.toDataObject(entity);
        return super.save(dataObject);
    }

}
