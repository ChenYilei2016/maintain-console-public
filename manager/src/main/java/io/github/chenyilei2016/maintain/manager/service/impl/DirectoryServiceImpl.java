package io.github.chenyilei2016.maintain.manager.service.impl;

import com.alibaba.fastjson2.JSON;
import io.github.chenyilei2016.maintain.manager.constant.ScriptPermissionEnum;
import io.github.chenyilei2016.maintain.manager.constant.TreeNodeTypeEnum;
import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;
import io.github.chenyilei2016.maintain.manager.controller.assembler.DirectoryNodeAssembler;
import io.github.chenyilei2016.maintain.manager.controller.dto.TreeNodeDeleteWebRequest;
import io.github.chenyilei2016.maintain.manager.controller.dto.TreeNodeMoveWebRequest;
import io.github.chenyilei2016.maintain.manager.controller.dto.TreeNodeSaveWebRequest;
import io.github.chenyilei2016.maintain.manager.exceptions.CommonException;
import io.github.chenyilei2016.maintain.manager.pojo.dto.DirectoryNodeDTO;
import io.github.chenyilei2016.maintain.manager.pojo.dto.ScriptNodeDTO;
import io.github.chenyilei2016.maintain.manager.pojo.dto.ScriptRevisionDTO;
import io.github.chenyilei2016.maintain.manager.pojo.entity.*;
import io.github.chenyilei2016.maintain.manager.pojo.repository.DirectoryNodeRepository;
import io.github.chenyilei2016.maintain.manager.pojo.repository.ScriptRepository;
import io.github.chenyilei2016.maintain.manager.pojo.repository.ScriptRevisionRepository;
import io.github.chenyilei2016.maintain.manager.pojo.vo.ScriptVO;
import io.github.chenyilei2016.maintain.manager.service.DirectoryService;
import io.github.chenyilei2016.maintain.manager.service.ScriptAccessControl;
import io.github.chenyilei2016.maintain.manager.service.ScriptContentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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

    private final DirectoryNodeRepository directoryNodeRepository;
    private final ScriptRepository scriptRepository;
    private final ScriptRevisionRepository scriptRevisionRepository;
    private final ScriptContentService scriptContentService;
    private final ScriptAccessControl access;

    public DirectoryServiceImpl(
            DirectoryNodeRepository directoryNodeRepository,
            ScriptRepository scriptRepository,
            ScriptRevisionRepository scriptRevisionRepository,
            ScriptContentService scriptContentService,
            ScriptAccessControl access
    ) {
        this.directoryNodeRepository = directoryNodeRepository;
        this.scriptRepository = scriptRepository;
        this.scriptRevisionRepository = scriptRevisionRepository;
        this.scriptContentService = scriptContentService;
        this.access = access;
    }

    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<DirectoryNodeDTO> getDirectoryTree(String serviceName, LocalLoginUser actor) {
        log.info("获取目录树结构，服务名：{}，创建人：{}", serviceName, actor.getEmployeeNo());

        List<DirectoryNode> allNodes = directoryNodeRepository.findServiceTree(serviceName);

        // 构建树形结构，只支持根目录和二级目录（最多两级）
        return buildDirectoryTreeWithMaxDepth(allNodes, 2, actor);
    }

    @Override
    public ScriptNodeDTO getScriptDetail(String scriptId, LocalLoginUser actor) {
        log.info("获取脚本详情，脚本ID：{}", scriptId);

        ScriptVO scriptVO = scriptContentService.findById(scriptId);
        boolean canRead = hasPermission(scriptVO, actor, ScriptPermissionEnum.READ);
        boolean canInvoke = hasPermission(scriptVO, actor, ScriptPermissionEnum.INVOKE);
        boolean canEdit = hasPermission(scriptVO, actor, ScriptPermissionEnum.EDIT);
        boolean canManage = hasPermission(scriptVO, actor, ScriptPermissionEnum.MANAGE);
        ScriptNodeDTO dto = canRead || canEdit ? convertToScriptNodeDTO(scriptVO) : minimalScriptNode(scriptVO);
        dto.setCanRead(canRead);
        dto.setCanInvoke(canInvoke);
        dto.setCanEdit(canEdit);
        dto.setCanManage(canManage);
        if (!dto.isCanManage()) dto.setPermissions(null);
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String treeNodeSave(TreeNodeSaveWebRequest request, LocalLoginUser actor) {
        request.setOperatorId(actor.getEmployeeNo());
        request.setOperatorName(actor.getEmployeeName());
        log.info("保存树节点, type:{}, id:{}, name:{}, service:{}",
                request.getNodeType(), request.getNodeId(), request.getNodeName(), request.getServiceName());

        // 验证节点类型
        TreeNodeTypeEnum nodeTypeEnum = TreeNodeTypeEnum.getEnumThrow(request.getNodeType());
        String nodeId = request.getNodeId();
        if (nodeId == null && request.getParentId() != null) {
            DirectoryNode parent = directoryNodeRepository.findById(request.getParentId());
            if (parent == null || !DirectoryNode.TYPE_FOLDER.equals(parent.getType())
                    || !parent.getServiceName().equals(request.getServiceName())) {
                throw CommonException.createReminderException("父目录不存在或不属于当前服务");
            }
        }

        if (TreeNodeTypeEnum.FOLDER == nodeTypeEnum) {
            // 处理文件夹
            return handleFolderSave(request, nodeId, actor);
        } else if (TreeNodeTypeEnum.SCRIPT == nodeTypeEnum) {
            // 处理脚本
            return handleScriptSave(request, nodeId, actor);
        } else {
            throw new IllegalArgumentException("不支持的节点类型：" + request.getNodeType());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean treeNodeDelete(TreeNodeDeleteWebRequest request, LocalLoginUser actor) {
        log.info("删除树节点，请求：{}", request);

        DirectoryNode node = directoryNodeRepository.findById(request.getNodeId());
        if (node == null) {
            throw new RuntimeException("节点不存在");
        }

        if (DirectoryNode.TYPE_SCRIPT.equals(node.getType())) {
            access.require(node.getId(), actor, ScriptPermissionEnum.MANAGE);
            return directoryNodeRepository.deleteById(node.getId());
        }
        if (!Objects.equals(node.getCreatorId(), request.getOperatorId())) {
            throw CommonException.createReminderException("只有创建者可以删除目录");
        }
        List<DirectoryNode> nodes = directoryNodeRepository.findServiceTree(node.getServiceName());
        Map<String, List<DirectoryNode>> children = nodes.stream()
                .filter(item -> item.getParentId() != null)
                .collect(Collectors.groupingBy(DirectoryNode::getParentId));
        if (!request.getForceDelete() && !children.getOrDefault(node.getId(), List.of()).isEmpty()) {
            throw CommonException.createReminderException("目录不为空，请明确确认包含子资源");
        }
        Set<String> ids = new LinkedHashSet<>();
        ArrayDeque<DirectoryNode> pending = new ArrayDeque<>();
        pending.add(node);
        while (!pending.isEmpty()) {
            DirectoryNode current = pending.removeFirst();
            if (DirectoryNode.TYPE_SCRIPT.equals(current.getType())) {
                if (!ScriptPermissionEntity.checkPermission(current,
                        new Script().setPermissions(current.getScriptPermissions()), actor, ScriptPermissionEnum.MANAGE)) {
                    throw CommonException.createReminderException("没有脚本授权管理权限");
                }
            } else if (!Objects.equals(current.getCreatorId(), request.getOperatorId())) {
                throw CommonException.createReminderException("目录包含其他人管理的资源，请先联系对应创建者");
            }
            if (!ids.add(current.getId()))
                throw CommonException.createReminderException("目录存在循环引用，无法安全删除");
            pending.addAll(children.getOrDefault(current.getId(), List.of()));
        }
        // 一次批量逻辑删除；源码、版本和执行历史保留，避免循环查库和破坏历史外键。
        return directoryNodeRepository.deleteAll(List.copyOf(ids));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String treeNodeMove(TreeNodeMoveWebRequest request, LocalLoginUser actor) {
        DirectoryNode node = directoryNodeRepository.findById(request.getNodeId());
        if (node == null) throw CommonException.createReminderException("节点不存在");

        if (DirectoryNode.TYPE_SCRIPT.equals(node.getType())) {
            access.require(node.getId(), actor, ScriptPermissionEnum.EDIT);
        } else if (!Objects.equals(node.getCreatorId(), actor.getEmployeeNo())) {
            throw CommonException.createReminderException("只有创建者可以移动目录");
        }

        String parentId = StringUtils.hasText(request.getParentId()) ? request.getParentId() : null;
        if (Objects.equals(node.getParentId(), parentId)) return node.getId();

        List<DirectoryNode> serviceTree = directoryNodeRepository.findServiceTree(node.getServiceName());
        Map<String, DirectoryNode> nodesById = serviceTree.stream()
                .collect(Collectors.toMap(DirectoryNode::getId, item -> item));
        DirectoryNode parent = parentId == null ? null : nodesById.get(parentId);
        if (parentId != null && (parent == null || !DirectoryNode.TYPE_FOLDER.equals(parent.getType()))) {
            throw CommonException.createReminderException("目标目录不存在或不属于当前服务");
        }

        if (DirectoryNode.TYPE_FOLDER.equals(node.getType())) {
            for (DirectoryNode current = parent; current != null; current = nodesById.get(current.getParentId())) {
                if (Objects.equals(current.getId(), node.getId())) {
                    throw CommonException.createReminderException("不能把目录移动到自身或其子目录");
                }
            }
            if (parent != null && (parent.getParentId() != null || serviceTree.stream().anyMatch(item ->
                    DirectoryNode.TYPE_FOLDER.equals(item.getType()) && Objects.equals(item.getParentId(), node.getId())))) {
                throw CommonException.createReminderException("移动后目录层级将超过2层");
            }
        }

        checkNameDuplicate(node.getName(), parentId, node.getServiceName());
        if (!directoryNodeRepository.updateParentId(node.getId(), parentId, LocalDateTime.now())) {
            throw CommonException.createReminderException("节点已删除，移动失败");
        }
        return node.getId();
    }

    /**
     * 处理文件夹保存（创建/更新）
     */
    private String handleFolderSave(TreeNodeSaveWebRequest request, String nodeId, LocalLoginUser actor) {
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
            if (!Objects.equals(existingFolder.getCreatorId(), actor.getEmployeeNo())) {
                throw CommonException.createReminderException("只有创建者可以修改文件夹");
            }
            if (!DirectoryNode.TYPE_FOLDER.equals(existingFolder.getType())) {
                throw CommonException.createReminderException("节点类型不匹配");
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
    private String handleScriptSave(TreeNodeSaveWebRequest request, String nodeId, LocalLoginUser actor) {
        if (nodeId == null) {
            return doScriptCreate(request);

        } else {
            doScriptUpdate(request, nodeId, actor);
        }

        return nodeId;
    }

    private String doScriptCreate(TreeNodeSaveWebRequest request) {
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
        scriptNode.setPermissionType(DirectoryNode.PERMISSION_PRIVATE);
        scriptNode.setCreateTime(LocalDateTime.now());
        scriptNode.setUpdateTime(LocalDateTime.now());
        scriptNode.setCreatorId(request.getOperatorId());
        scriptNode.setCreatorName(request.getOperatorName());

        // 保存目录节点并获取返回的实体（包含生成的ID）
        DirectoryNode savedScriptNode = directoryNodeRepository.save(scriptNode);

        // 创建脚本内容，使用保存后的节点ID
        Script script = new Script();
        script.setId(savedScriptNode.getId());
        String content = StringUtils.hasText(request.getContent()) ? request.getContent() : "// 新建脚本\nreturn \"Hello World\";";
        script.setContent(content);
        script.setParameterSchema(normalizeParameterSchema(request.getParameterSchema(), content));
        ScriptPermissionEntity permissions = ScriptPermissionEntity.privateTool(request.getOperatorId());
        permissions.setAllowedEnvironments(request.getAllowedEnvironments() == null ? List.of() : request.getAllowedEnvironments());
        script.setPermissions(JSON.toJSONString(permissions));
        script.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription() : request.getNodeName());
        script.setToolMetadata(JSON.toJSONString(request.getToolMetadata() == null ? new ScriptToolMetadata() : request.getToolMetadata()));
        script.setVersion(1);
        script.setCreateTime(LocalDateTime.now());
        script.setUpdateTime(LocalDateTime.now());
        Script savedScript = scriptRepository.insert(script);
        scriptRevisionRepository.saveRevision(savedScript, request.getOperatorId(), request.getOperatorName());

        log.info("创建脚本成功，ID：{}，名称：{}，创建人：{}", savedScriptNode.getId(), request.getNodeName(), request.getOperatorName());

        // 返回保存后的节点ID
        return savedScriptNode.getId();
    }

    private void doScriptUpdate(TreeNodeSaveWebRequest request, String nodeId, LocalLoginUser actor) {
        ScriptVO current = scriptContentService.findById(nodeId);
        DirectoryNode node = current.getDirectoryNode();
        Script script = current.getScript();
        if (!node.getServiceName().equals(request.getServiceName())
                || !hasPermission(current, actor, ScriptPermissionEnum.EDIT)) {
            throw CommonException.createReminderException("没有编辑权限或脚本所属服务不匹配");
        }
        if (!Objects.equals(request.getExpectedVersion(), script.getVersion())) {
            throw CommonException.createReminderException("保存冲突：工具已更新或请求缺少起始版本，请刷新并比较差异");
        }
        if (request.getPermissions() != null && !request.getPermissions().equals(script.getPermissions())) {
            throw CommonException.createReminderException("保存内容不能修改授权，请使用授权设置");
        }
        if (!request.getNodeName().equals(node.getName())) {
            checkNameDuplicate(request.getNodeName(), node.getParentId(), node.getServiceName());
            node.setName(request.getNodeName());
        }
        String content = request.getContent() == null ? script.getContent() : request.getContent();
        if (!StringUtils.hasText(content)) throw CommonException.createReminderException("脚本内容不能为空");
        script.setContent(content);
        script.setParameterSchema(normalizeParameterSchema(
                request.getParameterSchema() == null ? script.getParameterSchema() : request.getParameterSchema(), content));
        if (request.getDescription() != null) script.setDescription(request.getDescription());
        if (request.getToolMetadata() != null) script.setToolMetadata(JSON.toJSONString(request.getToolMetadata()));
        script.setUpdateTime(LocalDateTime.now());
        Script saved = scriptRepository.save(script, true);
        if (saved == null) throw CommonException.createReminderException("保存冲突，请刷新并比较差异");
        node.setUpdateTime(saved.getUpdateTime());
        directoryNodeRepository.save(node);
        scriptRevisionRepository.saveRevision(saved, request.getOperatorId(), request.getOperatorName());
    }

    private static String normalizeParameterSchema(String parameterSchema, String scriptContent) {
        ScriptParameterSchema schema = ScriptParameterSchema.parse(parameterSchema);
        return schema == null ? null : schema.validateForScript(scriptContent);
    }

    @Override
    public List<ScriptRevisionDTO> listScriptRevisions(String scriptId, LocalLoginUser actor) {
        ScriptVO scriptVO = scriptContentService.findById(scriptId);
        if (scriptVO == null || !hasPermission(scriptVO, actor, ScriptPermissionEnum.READ)) {
            throw CommonException.createReminderException("脚本不存在或没有读取权限");
        }
        return scriptRevisionRepository.listRecent(scriptId, 50).stream().map(ScriptRevisionDTO::from).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer restoreScriptRevision(String scriptId, int version, int expectedVersion, LocalLoginUser actor) {
        ScriptVO scriptVO = scriptContentService.findById(scriptId);
        if (scriptVO == null || !hasPermission(scriptVO, actor, ScriptPermissionEnum.EDIT)) {
            throw CommonException.createReminderException("脚本不存在或没有编辑权限");
        }
        ScriptRevision revision = scriptRevisionRepository.findRevision(scriptId, version);
        if (revision == null) {
            throw CommonException.createReminderException("脚本版本不存在: {}", version);
        }
        Script script = scriptVO.getScript();
        if (script.getVersion() != expectedVersion) {
            throw CommonException.createReminderException("恢复冲突：工具已更新，请刷新并比较差异");
        }
        script.setContent(revision.getContent());
        script.setParameterSchema(revision.getParameterSchema());
        script.setToolMetadata(revision.getToolMetadata());
        script.setDescription(revision.getDescription());
        script.setUpdateTime(LocalDateTime.now());
        Script restored = scriptRepository.save(script, true);
        if (restored == null) {
            throw CommonException.createReminderException("恢复脚本版本冲突，请刷新后重试");
        }
        scriptRevisionRepository.saveRevision(restored, actor.getEmployeeNo(), actor.getEmployeeName());
        return restored.getVersion();
    }

    private boolean hasPermission(ScriptVO scriptVO, LocalLoginUser actor, ScriptPermissionEnum permission) {
        return access.allows(scriptVO, actor, permission);
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
    private List<DirectoryNodeDTO> buildDirectoryTreeWithMaxDepth(List<DirectoryNode> nodes, int maxDepth,
                                                                  LocalLoginUser actor) {
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
                    DirectoryNodeDTO dto = convertToDirectoryNodeDTO(node, actor);
                    dto.setLevel(0);  // 设置根节点层级为0
                    setChildrenWithMaxDepth(dto, parentNodeMap, 1, maxDepth, actor);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 递归设置子节点（限制最大深度）
     */
    private void setChildrenWithMaxDepth(DirectoryNodeDTO parent, Map<String, List<DirectoryNode>> parentNodeMap,
                                         int currentDepth, int maxDepth, LocalLoginUser actor) {
        //这里是大于号, 因为第二层下的内容还需要展示
        if (currentDepth > maxDepth) {
            return; // 达到最大深度，停止递归
        }

        List<DirectoryNode> children = parentNodeMap.get(parent.getId() == null ? "null" : parent.getId());
        if (children != null && !children.isEmpty()) {
            List<DirectoryNodeDTO> childDTOs = children.stream()
                    .map(node -> {
                        DirectoryNodeDTO dto = convertToDirectoryNodeDTO(node, actor);
                        dto.setLevel(currentDepth);  // 设置子节点的层级
                        return dto;
                    })
                    .collect(Collectors.toList());

            parent.setChildren(childDTOs);

            // 递归设置子节点的子节点
            childDTOs.forEach(child -> setChildrenWithMaxDepth(child, parentNodeMap,
                    currentDepth + 1, maxDepth, actor));
        }
    }

    /**
     * 实体转DTO
     */
    private ScriptNodeDTO convertToScriptNodeDTO(ScriptVO vo) {
        return DirectoryNodeAssembler.INSTANCE.convert2ScriptNodeDTO(vo);
    }

    private ScriptNodeDTO minimalScriptNode(ScriptVO vo) {
        ScriptNodeDTO dto = new ScriptNodeDTO();
        dto.setId(vo.getDirectoryNode().getId());
        dto.setName(vo.getDirectoryNode().getName());
        dto.setType(vo.getDirectoryNode().getType());
        dto.setParentId(vo.getDirectoryNode().getParentId());
        dto.setServiceName(vo.getDirectoryNode().getServiceName());
        dto.setVersion(vo.getScript().getVersion());
        dto.setContent("");
        return dto;
    }

    private DirectoryNodeDTO convertToDirectoryNodeDTO(DirectoryNode node, LocalLoginUser actor) {
        DirectoryNodeDTO dto = DirectoryNodeAssembler.INSTANCE.convert2DirectoryNodeDTO(node);
        dto.setCreator(StringUtils.hasText(node.getCreatorName()) ? node.getCreatorName() : node.getCreatorId());
        if (!DirectoryNode.TYPE_SCRIPT.equals(node.getType())) {
            boolean owner = Objects.equals(node.getCreatorId(), actor.getEmployeeNo());
            dto.setCanCreateChild(true);
            dto.setCanRename(owner);
            dto.setCanDelete(owner);
            return dto;
        }
        try {
            ScriptPermissionEntity grants = ScriptPermissionEntity.parse(node.getScriptPermissions());
            dto.setCanRead(grants.allows(node, actor, ScriptPermissionEnum.READ));
            dto.setCanEdit(grants.allows(node, actor, ScriptPermissionEnum.EDIT));
            dto.setCanInvoke(grants.allows(node, actor, ScriptPermissionEnum.INVOKE));
            dto.setCanManage(grants.allows(node, actor, ScriptPermissionEnum.MANAGE));
            dto.setCanRename(dto.isCanEdit());
            dto.setCanDelete(dto.isCanManage());
        } catch (RuntimeException invalidPermissions) {
            log.warn("目录脚本权限配置无效，按无权限展示, scriptId:{}", node.getId());
        }
        return dto;
    }
}
