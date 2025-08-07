package io.github.chenyilei2016.maintain.manager.service.impl;

import io.github.chenyilei2016.maintain.manager.exceptions.CommonException;
import io.github.chenyilei2016.maintain.manager.pojo.entity.DirectoryNode;
import io.github.chenyilei2016.maintain.manager.pojo.entity.Script;
import io.github.chenyilei2016.maintain.manager.pojo.repository.DirectoryNodeRepository;
import io.github.chenyilei2016.maintain.manager.pojo.repository.ScriptRepository;
import io.github.chenyilei2016.maintain.manager.pojo.vo.ScriptVO;
import io.github.chenyilei2016.maintain.manager.service.ScriptContentService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author chenyilei
 * @since 2025/08/06 11:38
 */
@Service
public class ScriptContentServiceImpl implements ScriptContentService {

    @Resource
    private ScriptRepository scriptRepository;

    @Resource
    private DirectoryNodeRepository directoryNodeRepository;

    @Override
    public ScriptVO findById(String scriptId) {
        Script script = scriptRepository.findByScriptId(scriptId);

        if (null == script) {
            throw CommonException.createReminderException("不存在的脚本");
        }

        DirectoryNode node = directoryNodeRepository.findById(scriptId);

        if (node == null) {
            throw CommonException.createReminderException("不存在的节点");
        }

        ScriptVO scriptVO = new ScriptVO();
        scriptVO.setScript(script);
        scriptVO.setDirectoryNode(node);
        return scriptVO;
    }
}
