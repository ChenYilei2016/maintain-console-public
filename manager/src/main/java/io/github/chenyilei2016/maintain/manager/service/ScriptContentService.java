package io.github.chenyilei2016.maintain.manager.service;

import io.github.chenyilei2016.maintain.manager.pojo.vo.ScriptVO;

/**
 * @author chenyilei
 * @since 2025/08/06 11:38
 */
public interface ScriptContentService {
    ScriptVO findById(String scriptId);
}
