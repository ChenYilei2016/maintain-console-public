package io.github.chenyilei2016.maintain.manager.controller.manager;

import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;
import io.github.chenyilei2016.maintain.manager.context.LoginUserContext;
import io.github.chenyilei2016.maintain.manager.controller.dto.GetScriptDetailWebRequest;
import io.github.chenyilei2016.maintain.manager.controller.dto.TreeNodeDeleteWebRequest;
import io.github.chenyilei2016.maintain.manager.controller.dto.TreeNodeSaveWebRequest;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxResult;
import io.github.chenyilei2016.maintain.manager.pojo.dto.DirectoryNodeDTO;
import io.github.chenyilei2016.maintain.manager.pojo.dto.ScriptNodeDTO;
import io.github.chenyilei2016.maintain.manager.service.DirectoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 目录管理控制器
 *
 * @author chenyilei
 * @since 2025/07/31 13:56
 */
@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/manager/directory")
public class ManagerDirectoryController {

    @Autowired
    private DirectoryService directoryService;

    /**
     * 获取目录树结构
     */
    @PostMapping("/tree")
    public AjaxResult<List<DirectoryNodeDTO>> getDirectoryTree(@RequestParam String serviceName) {
        LocalLoginUser loginUser = LoginUserContext.getUser();
        List<DirectoryNodeDTO> result = directoryService.getDirectoryTree(serviceName, loginUser.getEmployeeName());
        return AjaxResult.success(result);
    }

    /**
     * 获取脚本详情
     */
    @PostMapping("/script/detail")
    public AjaxResult<ScriptNodeDTO> getScriptDetail(@RequestBody GetScriptDetailWebRequest request) {
        ScriptNodeDTO script = directoryService.getScriptDetail(request.getScriptId(), LoginUserContext.getUser().getEmployeeNo());
        return AjaxResult.success(script);
    }

    /**
     * 统一的树节点保存接口（创建/更新 文件夹/脚本）
     * 根据nodeType和nodeId判断具体操作：
     * - nodeId为空：创建新节点
     * - nodeId不为空：更新现有节点
     * - nodeType=folder：文件夹操作
     * - nodeType=script：脚本操作
     */
    @PostMapping("/treeNode/save")
    public AjaxResult<String> treeNodeSave(@RequestBody @Valid TreeNodeSaveWebRequest request) {
        LocalLoginUser loginUser = LoginUserContext.getUser();

        // 将操作人信息设置到请求中
        request.setOperatorId(loginUser.getEmployeeNo());
        request.setOperatorName(loginUser.getEmployeeName());

        log.info("保存树节点: {}, 操作人: {}({})", request, loginUser.getEmployeeName(), loginUser.getEmployeeNo());

        String result = directoryService.treeNodeSave(request);

        String operation = request.getNodeId() == null ? "创建" : "更新";
        String nodeTypeName = "folder".equals(request.getNodeType()) ? "文件夹" : "脚本";

        return AjaxResult.success(result, operation + nodeTypeName + "成功");
    }

    /**
     * 统一的树节点删除接口（删除 文件夹/脚本）
     * 根据节点ID自动判断节点类型：
     * - 文件夹：检查是否包含子节点，根据forceDelete决定是否强制删除
     * - 脚本：直接删除
     */
    @PostMapping("/treeNode/delete")
    public AjaxResult<String> treeNodeDelete(@RequestBody @Valid TreeNodeDeleteWebRequest request) {
        LocalLoginUser loginUser = LoginUserContext.getUser();

        // 将操作人信息设置到请求中
        request.setOperatorId(loginUser.getEmployeeNo());
        request.setOperatorName(loginUser.getEmployeeName());

        log.info("删除树节点: {}, 操作人: {}({})", request, loginUser.getEmployeeName(), loginUser.getEmployeeNo());

        boolean success = directoryService.treeNodeDelete(request);

        if (success) {
            return AjaxResult.success("删除成功");
        } else {
            return AjaxResult.error("删除失败");
        }
    }
}
