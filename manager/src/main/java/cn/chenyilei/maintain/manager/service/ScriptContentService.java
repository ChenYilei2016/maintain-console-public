package cn.chenyilei.maintain.manager.service;

import cn.chenyilei.maintain.manager.pojo.vo.ScriptVO;

/**
 * @author chenyilei
 * @since 2025/08/06 11:38
 */
public interface ScriptContentService {
    ScriptVO findById(String scriptId);
}
