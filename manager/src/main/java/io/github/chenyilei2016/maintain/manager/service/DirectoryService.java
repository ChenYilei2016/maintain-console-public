package io.github.chenyilei2016.maintain.manager.service;

import io.github.chenyilei2016.maintain.manager.controller.dto.TreeNodeDeleteWebRequest;
import io.github.chenyilei2016.maintain.manager.controller.dto.TreeNodeSaveWebRequest;
import io.github.chenyilei2016.maintain.manager.pojo.dto.DirectoryNodeDTO;
import io.github.chenyilei2016.maintain.manager.pojo.dto.ScriptNodeDTO;

import java.util.List;

/**
 * 目录管理服务接口
 *
 * @author chenyilei
 * @since 2025/07/31
 */
public interface DirectoryService {

    /**
     * 获取目录树结构
     *
     * @param serviceName 服务名
     * @param creator     创建人（可选，用于过滤我的脚本）
     * @return 目录树结构
     */
    List<DirectoryNodeDTO> getDirectoryTree(String serviceName, String creator);

    /**
     * 获取脚本详情
     *
     * @param scriptId 脚本ID
     * @return 脚本详情
     */
    ScriptNodeDTO getScriptDetail(String scriptId, String employeeNo);

    /**
     * 统一保存树节点（文件夹/脚本的创建和更新）
     *
     * @param request 保存请求（包含操作人信息）
     * @return 节点ID
     */
    String treeNodeSave(TreeNodeSaveWebRequest request);

    /**
     * 统一删除树节点（文件夹/脚本的删除）
     *
     * @param request 删除请求（包含操作人信息）
     * @return 是否成功
     */
    boolean treeNodeDelete(TreeNodeDeleteWebRequest request);
}