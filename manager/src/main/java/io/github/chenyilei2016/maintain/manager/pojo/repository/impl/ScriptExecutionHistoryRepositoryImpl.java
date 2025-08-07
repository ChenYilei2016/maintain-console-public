package io.github.chenyilei2016.maintain.manager.pojo.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.chenyilei2016.maintain.manager.pojo.converter.ScriptExecutionHistoryConverter;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ScriptExecutionHistoryDO;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptExecutionHistoryEntity;
import io.github.chenyilei2016.maintain.manager.pojo.mapper.ScriptExecutionHistoryMapper;
import io.github.chenyilei2016.maintain.manager.pojo.repository.ScriptExecutionHistoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 脚本执行历史 仓库实现类
 *
 * @author chenyilei
 * @since 2025/08/05
 */
@Repository
public class ScriptExecutionHistoryRepositoryImpl extends ServiceImpl<ScriptExecutionHistoryMapper, ScriptExecutionHistoryDO> implements ScriptExecutionHistoryRepository {

    @Override
    public IPage<ScriptExecutionHistoryEntity> page(IPage page, QueryWrapper<ScriptExecutionHistoryDO> queryWrapper) {
        long total = baseMapper.selectCount(queryWrapper); // Simplified count
        if (total == 0) {
            return page;
        }

        List<ScriptExecutionHistoryDO> records = baseMapper.selectList(page, queryWrapper);
        List<ScriptExecutionHistoryEntity> entityRecords = records.stream()
                .map(ScriptExecutionHistoryConverter.INSTANCE::toEntity)
                .collect(Collectors.toList());

        page.setRecords(entityRecords);
        page.setTotal(total);
        return page;
    }

    public boolean save(ScriptExecutionHistoryEntity entity) {
        ScriptExecutionHistoryDO dataObject = ScriptExecutionHistoryConverter.INSTANCE.toDataObject(entity);
        return super.save(dataObject);
    }

}
