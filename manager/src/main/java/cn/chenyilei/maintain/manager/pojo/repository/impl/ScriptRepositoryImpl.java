package cn.chenyilei.maintain.manager.pojo.repository.impl;

import cn.chenyilei.maintain.manager.pojo.dataobject.ScriptDO;
import cn.chenyilei.maintain.manager.pojo.entity.Script;
import cn.chenyilei.maintain.manager.pojo.mapper.ScriptMapper;
import cn.chenyilei.maintain.manager.pojo.repository.ScriptRepository;
import cn.chenyilei.maintain.manager.pojo.repository.converter.ScriptContentConverter;
import cn.chenyilei.maintain.manager.utils.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Repository;

/**
 * 脚本内容仓储实现类
 * 作为防腐层，负责数据对象与业务实体的转换
 *
 * @author chenyilei
 * @since 2025/07/31
 */
@Repository
public class ScriptRepositoryImpl extends ServiceImpl<ScriptMapper, ScriptDO> implements ScriptRepository {

    private final ScriptContentConverter converter = ScriptContentConverter.INSTANCE;

    @Override
    public Script findByScriptId(String scriptId) {
        ScriptDO dataObject = baseMapper.selectByScriptId(scriptId);
        return converter.toEntity(dataObject);
    }

    @Override
    public Script insert(Script script) {
        ScriptDO dataObject = converter.toDataObject(script);
        if (dataObject.getId() == null) {
            dataObject.setId(IdUtil.generateSnowFlakeId());
            super.save(dataObject);
        } else {
            super.save(dataObject);
        }
        return converter.toEntity(dataObject);
    }

    @Override
    public Script save(Script script, Boolean versionLock) {
        ScriptDO dataObject = converter.toDataObject(script);
        if (dataObject.getId() == null) {
            dataObject.setId(IdUtil.generateSnowFlakeId());
            super.save(dataObject);
        } else {
            LambdaQueryWrapper<ScriptDO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ScriptDO::getId, dataObject.getId());
            if (versionLock) {
                wrapper.eq(ScriptDO::getVersion, dataObject.getVersion());
                dataObject.setVersion(dataObject.getVersion() + 1);
            }
            int update = baseMapper.update(dataObject, wrapper);

            if (update == 0) {
                return null;
            }
        }
        return converter.toEntity(dataObject);
    }


    @Override
    public void deleteById(String id) {
        removeById(id);
    }
}