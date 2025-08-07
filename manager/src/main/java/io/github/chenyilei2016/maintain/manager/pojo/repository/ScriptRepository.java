package io.github.chenyilei2016.maintain.manager.pojo.repository;

import io.github.chenyilei2016.maintain.manager.pojo.entity.Script;

/**
 * 脚本内容仓储接口
 *
 * @author chenyilei
 * @since 2025/07/31
 */
public interface ScriptRepository {

    /**
     * 根据脚本ID查询脚本内容
     */
    Script findByScriptId(String scriptId);

    Script insert(Script script);

    /**
     * 保存或更新脚本内容
     */
    Script save(Script script, Boolean versionLock);


    void deleteById(String id);
}