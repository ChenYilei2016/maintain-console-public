package cn.chenyilei.maintain.manager.service.impl;

import cn.chenyilei.maintain.manager.constant.ScriptPermissionEnum;
import cn.chenyilei.maintain.manager.constant.TreeNodeTypeEnum;
import cn.chenyilei.maintain.manager.controller.assembler.DirectoryNodeAssembler;
import cn.chenyilei.maintain.manager.controller.dto.TreeNodeDeleteWebRequest;
import cn.chenyilei.maintain.manager.controller.dto.TreeNodeSaveWebRequest;
import cn.chenyilei.maintain.manager.exceptions.CommonException;
import cn.chenyilei.maintain.manager.pojo.dto.DirectoryNodeDTO;
import cn.chenyilei.maintain.manager.pojo.dto.ScriptNodeDTO;
import cn.chenyilei.maintain.manager.pojo.entity.DirectoryNode;
import cn.chenyilei.maintain.manager.pojo.entity.Script;
import cn.chenyilei.maintain.manager.pojo.entity.ScriptPermissionEntity;
import cn.chenyilei.maintain.manager.pojo.repository.DirectoryNodeRepository;
import cn.chenyilei.maintain.manager.pojo.repository.ScriptRepository;
import cn.chenyilei.maintain.manager.pojo.vo.ScriptVO;
import cn.chenyilei.maintain.manager.service.DirectoryService;
import cn.chenyilei.maintain.manager.service.ScriptContentService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 目录管理服务实现类
 *
 * @author chenyilei
 * @since 2025/07/31
 */
@Slf4j
@Service
public class DirectoryServiceImpl implements DirectoryService {

    @Autowired
    private DirectoryNodeRepository directoryNodeRepository;

    @Autowired
    private ScriptRepository scriptRepository;

    @Resource
    private ScriptContentService scriptContentService;

    @Resource
    private DirectoryServiceImpl self;

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<DirectoryNodeDTO> getDirectoryTree(String serviceName, String creator) {
        log.info("获取目录树结构，服务名：{}，创建人：{}", serviceName, creator);

        // 查询用户可见的所有节点（所有文件夹 + 公共脚本 + 用户的私有脚本）
        List<DirectoryNode> allNodes = directoryNodeRepository.findVisibleByServiceNameAndCreator(serviceName, creator);

        // 构建树形结构，只支持根目录和二级目录（最多两级）
        return buildDirectoryTreeWithMaxDepth(allNodes, 2);
    }

    @Override
    public ScriptNodeDTO getScriptDetail(String scriptId, String creatorId) {
        log.info("获取脚本详情，脚本ID：{}", scriptId);

        ScriptVO scriptVO = scriptContentService.findById(scriptId);

        ScriptNodeDTO dto = convertToScriptNodeDTO(scriptVO);
        //填充权限
        dto.setCanRead(ScriptPermissionEntity.checkPermission(scriptVO.getDirectoryNode(), scriptVO.getScript(), creatorId, ScriptPermissionEnum.READ));
        dto.setCanInvoke(ScriptPermissionEntity.checkPermission(scriptVO.getDirectoryNode(), scriptVO.getScript(), creatorId, ScriptPermissionEnum.INVOKE));
        dto.setCanEdit(ScriptPermissionEntity.checkPermission(scriptVO.getDirectoryNode(), scriptVO.getScript(), creatorId, ScriptPermissionEnum.EDIT));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String treeNodeSave(TreeNodeSaveWebRequest request) {
        log.info("保存树节点，请求：{}", request);

        // 验证节点类型
        TreeNodeTypeEnum nodeTypeEnum = TreeNodeTypeEnum.getEnumThrow(request.getNodeType());
        String nodeId = request.getNodeId();

        if (TreeNodeTypeEnum.FOLDER == nodeTypeEnum) {
            // 处理文件夹
            return handleFolderSave(request, nodeId);
        } else if (TreeNodeTypeEnum.SCRIPT == nodeTypeEnum) {
            // 处理脚本
            return handleScriptSave(request, nodeId);
        } else {
            throw new IllegalArgumentException("不支持的节点类型：" + request.getNodeType());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean treeNodeDelete(TreeNodeDeleteWebRequest request) {
        log.info("删除树节点，请求：{}", request);

        DirectoryNode node = directoryNodeRepository.findById(request.getNodeId());
        if (node == null) {
            throw new RuntimeException("节点不存在");
        }

        node.checkThrowAuth(request.getOperatorId());

        if (DirectoryNode.TYPE_FOLDER.equals(node.getType())) {
            // 处理文件夹删除
            return handleFolderDelete(request, node);
        } else if (DirectoryNode.TYPE_SCRIPT.equals(node.getType())) {
            // 处理脚本删除
            return handleScriptDelete(request, node);
        } else {
            throw new IllegalArgumentException("不支持的节点类型：" + node.getType());
        }
    }

    /**
     * 处理文件夹保存（创建/更新）
     */
    private String handleFolderSave(TreeNodeSaveWebRequest request, String nodeId) {
        if (nodeId == null) {
            // 验证是否超过最大层级深度
            if (request.getParentId() != null) {
                DirectoryNode parentNode = directoryNodeRepository.findById(request.getParentId());
                if (parentNode != null && parentNode.getParentId() != null) {
                    throw CommonException.createReminderException("目录层级不能超过2层，无法在此位置创建文件夹");
                }
            }

            // 检查名称重复
            checkNameDuplicate(request.getNodeName(), request.getParentId(), request.getServiceName());

            DirectoryNode folder = new DirectoryNode();
            folder.setId(nodeId);
            folder.setName(request.getNodeName());
            folder.setType(DirectoryNode.TYPE_FOLDER);
            folder.setParentId(request.getParentId());
            folder.setServiceName(request.getServiceName());
            folder.setSortOrder(0);
            folder.setCreatorId(request.getOperatorName());
            folder.setPermissionType(DirectoryNode.PERMISSION_PUBLIC); // 文件夹始终为公共权限
            folder.setCreateTime(LocalDateTime.now());
            folder.setUpdateTime(LocalDateTime.now());
            folder.setCreatorId(request.getOperatorId());
            folder.setCreatorName(request.getOperatorName());

            // 保存文件夹并获取返回的实体（包含生成的ID）
            DirectoryNode savedFolder = directoryNodeRepository.save(folder);
            log.info("创建文件夹成功，ID：{}，名称：{}，创建人：{}", savedFolder.getId(), request.getNodeName(), request.getOperatorName());

            // 返回保存后的文件夹ID
            return savedFolder.getId();
        } else {
            // 更新文件夹
            DirectoryNode existingFolder = directoryNodeRepository.findById(nodeId);
            if (existingFolder == null) {
                throw new RuntimeException("文件夹不存在");
            }

            // 检查名称重复（如果名称有变化）
            if (!request.getNodeName().equals(existingFolder.getName())) {
                checkNameDuplicate(request.getNodeName(), existingFolder.getParentId(), existingFolder.getServiceName());
                existingFolder.setName(request.getNodeName());
                existingFolder.setUpdateTime(LocalDateTime.now());
                directoryNodeRepository.save(existingFolder);
                log.info("更新文件夹成功，ID：{}，新名称：{}，操作人：{}", nodeId, request.getNodeName(), request.getOperatorName());
            }
        }

        return nodeId;
    }

    /**
     * 处理脚本保存（创建/更新）
     */
    private String handleScriptSave(TreeNodeSaveWebRequest request, String nodeId) {
        if (nodeId == null) {
            return self.doScriptCreate(request);

        } else {
            self.doScriptUpdate(request, nodeId);
        }

        return nodeId;
    }

    @Transactional(rollbackFor = Throwable.class)
    public String doScriptCreate(TreeNodeSaveWebRequest request) {
        // 检查名称重复
        checkNameDuplicate(request.getNodeName(), request.getParentId(), request.getServiceName());

        // 创建目录节点
        DirectoryNode scriptNode = new DirectoryNode();
        scriptNode.setName(request.getNodeName());
        scriptNode.setType(DirectoryNode.TYPE_SCRIPT);
        scriptNode.setParentId(request.getParentId());
        scriptNode.setServiceName(request.getServiceName());
        scriptNode.setSortOrder(0);
        scriptNode.setCreatorId(request.getOperatorName());
        scriptNode.setPermissionType(DirectoryNode.PERMISSION_PUBLIC); // 脚本默认为公共权限
        scriptNode.setCreateTime(LocalDateTime.now());
        scriptNode.setUpdateTime(LocalDateTime.now());
        scriptNode.setCreatorId(request.getOperatorId());
        scriptNode.setCreatorName(request.getOperatorName());

        // 保存目录节点并获取返回的实体（包含生成的ID）
        DirectoryNode savedScriptNode = directoryNodeRepository.save(scriptNode);

        // 创建脚本内容，使用保存后的节点ID
        Script script = new Script();
        script.setId(savedScriptNode.getId());
        script.setContent(StringUtils.hasText(request.getContent()) ? request.getContent() : "// 新建脚本\nreturn \"Hello World\";");
        try {
            ScriptPermissionEntity scriptPermissionEntity = JSON.parseObject(StringUtils.hasText(request.getPermissions()) ? request.getPermissions() : ScriptPermissionEntity.init(request.getOperatorId())
                    , ScriptPermissionEntity.class);
            script.setPermissions(JSON.toJSONString(scriptPermissionEntity));
        } catch (Exception e) {
            throw CommonException.createReminderException("permission json解析失败");
        }
        script.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription() : request.getNodeName());
        script.setVersion(1);
        script.setCreateTime(LocalDateTime.now());
        script.setUpdateTime(LocalDateTime.now());
        scriptRepository.insert(script);

        log.info("创建脚本成功，ID：{}，名称：{}，创建人：{}", savedScriptNode.getId(), request.getNodeName(), request.getOperatorName());

        // 返回保存后的节点ID
        return savedScriptNode.getId();
    }

    @Transactional(rollbackFor = Throwable.class)
    public void doScriptUpdate(TreeNodeSaveWebRequest request, String nodeId) {
        // 更新脚本
        DirectoryNode existingNode = directoryNodeRepository.findById(nodeId);
        if (existingNode == null) {
            throw new RuntimeException("脚本不存在");
        }

        existingNode.checkThrowAuth(request.getOperatorId());

        // 更新目录节点名称（如果有变化）
        if (!request.getNodeName().equals(existingNode.getName())) {
            checkNameDuplicate(request.getNodeName(), existingNode.getParentId(), existingNode.getServiceName());
            existingNode.setName(request.getNodeName());
            existingNode.setUpdateTime(LocalDateTime.now());
            directoryNodeRepository.save(existingNode);
        }

        // 更新脚本内容（带乐观锁机制）
        Script existingScript = scriptRepository.findByScriptId(nodeId);
        if (existingScript == null) {
            throw CommonException.createReminderException("脚本异常, 请删除此节点");
        } else {

            if (!ScriptPermissionEntity.checkPermission(existingNode, existingScript, request.getOperatorId(), ScriptPermissionEnum.EDIT)) {
                throw CommonException.createReminderException("没有权限进行此操作:{},{}", request.getOperatorId(), "EDIT");
            }
            // 更新现有脚本内容
            boolean contentChanged = isScriptContentChanged(request, existingScript);
            if (contentChanged) {
                existingScript.setUpdateTime(LocalDateTime.now());
                Script u = scriptRepository.save(existingScript, true);
                if (u == null) {
                    throw CommonException.createReminderException("更新脚本冲突, 存在抢占行为");
                }
                log.info("脚本内容更新成功，版本号：{} -> {}，操作人：{}",
                        existingScript.getVersion(), existingScript.getVersion(), request.getOperatorName());
            }
        }

        log.info("更新脚本成功，ID：{}，名称：{}，操作人：{}", nodeId, request.getNodeName(), request.getOperatorName());
    }

    private static boolean isScriptContentChanged(TreeNodeSaveWebRequest request, Script existingScript) {
        boolean contentChanged = false;
        if (StringUtils.hasText(request.getContent()) && !request.getContent().equals(existingScript.getContent())) {
            existingScript.setContent(request.getContent());
            contentChanged = true;
        }
        if (StringUtils.hasText(request.getPermissions()) && !request.getPermissions().equals(existingScript.getPermissions())) {
            try {
                ScriptPermissionEntity scriptPermissionEntity = JSON.parseObject(StringUtils.hasText(request.getPermissions()) ? request.getPermissions() : ScriptPermissionEntity.init(request.getOperatorId())
                        , ScriptPermissionEntity.class);
                existingScript.setPermissions(JSON.toJSONString(scriptPermissionEntity));
            } catch (Exception e) {
                throw CommonException.createReminderException("permission json解析失败");
            }
            contentChanged = true;
        }
        if (StringUtils.hasText(request.getDescription()) && !request.getDescription().equals(existingScript.getDescription())) {
            existingScript.setDescription(request.getDescription());
            contentChanged = true;
        }
        return contentChanged;
    }

    /**
     * 处理文件夹删除
     */
    private boolean handleFolderDelete(TreeNodeDeleteWebRequest request, DirectoryNode folder) {
        // 检查文件夹是否包含子节点
        List<DirectoryNode> children = directoryNodeRepository.findByParentId(folder.getId());

        if (!children.isEmpty() && !request.getForceDelete()) {
            throw new RuntimeException("文件夹不为空，无法删除。如需强制删除，请设置forceDelete为true");
        }

        if (request.getForceDelete()) {
            // 强制删除：递归删除所有子节点
            deleteNodeRecursively(folder.getId(), request.getOperatorName());
            log.info("强制删除文件夹及所有子节点成功，ID：{}，操作人：{}", folder.getId(), request.getOperatorName());
        } else {
            // 非强制删除：直接删除空文件夹
            directoryNodeRepository.deleteById(folder.getId());
            log.info("删除空文件夹成功，ID：{}，操作人：{}", folder.getId(), request.getOperatorName());
        }

        return true;
    }

    /**
     * 处理脚本删除
     */
    private boolean handleScriptDelete(TreeNodeDeleteWebRequest request, DirectoryNode script) {
        // 删除脚本内容
        Script scriptEntity = scriptRepository.findByScriptId(script.getId());
        if (scriptEntity != null) {
            scriptRepository.deleteById(scriptEntity.getId());
        }

        // 删除目录节点
        directoryNodeRepository.deleteById(script.getId());

        log.info("删除脚本成功，ID：{}，名称：{}，操作人：{}", script.getId(), script.getName(), request.getOperatorName());

        return true;
    }

    /**
     * 递归删除节点及其所有子节点
     */
    private void deleteNodeRecursively(String nodeId, String operator) {
        DirectoryNode node = directoryNodeRepository.findById(nodeId);
        if (node == null) {
            return;
        }

        // 先删除所有子节点
        List<DirectoryNode> children = directoryNodeRepository.findByParentId(nodeId);
        for (DirectoryNode child : children) {
            deleteNodeRecursively(child.getId(), operator);
        }

        // 删除当前节点
        if (DirectoryNode.TYPE_SCRIPT.equals(node.getType())) {
            // 删除脚本内容
            Script scriptEntity = scriptRepository.findByScriptId(nodeId);
            if (scriptEntity != null) {
                scriptRepository.deleteById(scriptEntity.getId());
            }
        }

        // 删除目录节点
        directoryNodeRepository.deleteById(nodeId);

        log.debug("递归删除节点：{}，类型：{}，操作人：{}", nodeId, node.getType(), operator);
    }

    /**
     * 检查名称重复
     */
    private void checkNameDuplicate(String name, String parentId, String serviceName) {
        List<DirectoryNode> existingNodes = directoryNodeRepository.findByNameAndParentIdAndServiceName(name, parentId, serviceName);
        if (!existingNodes.isEmpty()) {
            throw CommonException.createReminderException("节点名称已存在");
        }
        log.debug("检查名称重复：{}，父节点：{}，服务：{}", name, parentId, serviceName);
    }

    /**
     * 构建目录树结构（限制最大深度）
     */
    private List<DirectoryNodeDTO> buildDirectoryTreeWithMaxDepth(List<DirectoryNode> nodes, int maxDepth) {
        if (nodes == null || nodes.isEmpty()) {
            return new ArrayList<>();
        }

        // 按父节点分组
        Map<String, List<DirectoryNode>> parentNodeMap = nodes.stream()
                .collect(Collectors.groupingBy(node -> node.getParentId() == null ? "null" : node.getParentId()));

        // 先找出根节点（parentId为null）
        List<DirectoryNode> rootNodes = nodes.stream()
                .filter(node -> node.getParentId() == null)
                .collect(Collectors.toList());

        // 构建树形结构
        return rootNodes.stream()
                .map(node -> {
                    DirectoryNodeDTO dto = convertToDirectoryNodeDTO(node);
                    dto.setLevel(0);  // 设置根节点层级为0
                    setChildrenWithMaxDepth(dto, parentNodeMap, 1, maxDepth);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 递归设置子节点（限制最大深度）
     */
    private void setChildrenWithMaxDepth(DirectoryNodeDTO parent, Map<String, List<DirectoryNode>> parentNodeMap, int currentDepth, int maxDepth) {
        //这里是大于号, 因为第二层下的内容还需要展示
        if (currentDepth > maxDepth) {
            return; // 达到最大深度，停止递归
        }

        List<DirectoryNode> children = parentNodeMap.get(parent.getId() == null ? "null" : parent.getId());
        if (children != null && !children.isEmpty()) {
            List<DirectoryNodeDTO> childDTOs = children.stream()
                    .map(node -> {
                        DirectoryNodeDTO dto = convertToDirectoryNodeDTO(node);
                        dto.setLevel(currentDepth);  // 设置子节点的层级
                        return dto;
                    })
                    .collect(Collectors.toList());

            parent.setChildren(childDTOs);

            // 递归设置子节点的子节点
            childDTOs.forEach(child -> setChildrenWithMaxDepth(child, parentNodeMap, currentDepth + 1, maxDepth));
        }
    }

    /**
     * 实体转DTO
     */
    private ScriptNodeDTO convertToScriptNodeDTO(ScriptVO vo) {
        return DirectoryNodeAssembler.INSTANCE.convert2ScriptNodeDTO(vo);
    }

    private DirectoryNodeDTO convertToDirectoryNodeDTO(DirectoryNode node) {
        return DirectoryNodeAssembler.INSTANCE.convert2DirectoryNodeDTO(node);
    }
}