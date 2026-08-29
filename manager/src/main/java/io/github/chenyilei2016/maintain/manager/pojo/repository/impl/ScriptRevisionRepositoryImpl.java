package io.github.chenyilei2016.maintain.manager.pojo.repository.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ScriptRevisionDO;
import io.github.chenyilei2016.maintain.manager.pojo.entity.Script;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptRevision;
import io.github.chenyilei2016.maintain.manager.pojo.mapper.ScriptRevisionMapper;
import io.github.chenyilei2016.maintain.manager.pojo.repository.ScriptRevisionRepository;
import io.github.chenyilei2016.maintain.manager.utils.IdUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class ScriptRevisionRepositoryImpl extends ServiceImpl<ScriptRevisionMapper, ScriptRevisionDO>
        implements ScriptRevisionRepository {

    @Override
    public void saveRevision(Script script, String creatorId, String creatorName) {
        ScriptRevisionDO revision = new ScriptRevisionDO();
        revision.setId(IdUtil.generateSnowFlakeId());
        revision.setScriptId(script.getId());
        revision.setVersion(script.getVersion());
        revision.setContent(script.getContent());
        revision.setParameterSchema(script.getParameterSchema());
        revision.setPermissions(script.getPermissions());
        revision.setDescription(script.getDescription());
        revision.setCreatorId(creatorId);
        revision.setCreatorName(creatorName);
        revision.setCreateTime(LocalDateTime.now());
        save(revision);
    }

    @Override
    public List<ScriptRevision> listRecent(String scriptId, int limit) {
        return baseMapper.selectRecent(scriptId, limit).stream().map(this::toEntity).toList();
    }

    @Override
    public ScriptRevision findRevision(String scriptId, int version) {
        return toEntity(baseMapper.selectRevision(scriptId, version));
    }

    private ScriptRevision toEntity(ScriptRevisionDO dataObject) {
        if (dataObject == null) return null;
        ScriptRevision revision = new ScriptRevision();
        BeanUtils.copyProperties(dataObject, revision);
        return revision;
    }
}
