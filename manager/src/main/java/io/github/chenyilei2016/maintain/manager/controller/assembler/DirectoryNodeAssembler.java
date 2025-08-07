package io.github.chenyilei2016.maintain.manager.controller.assembler;

import io.github.chenyilei2016.maintain.manager.pojo.dto.DirectoryNodeDTO;
import io.github.chenyilei2016.maintain.manager.pojo.dto.ScriptNodeDTO;
import io.github.chenyilei2016.maintain.manager.pojo.entity.DirectoryNode;
import io.github.chenyilei2016.maintain.manager.pojo.entity.Script;
import io.github.chenyilei2016.maintain.manager.pojo.vo.ScriptVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.BeanUtils;

import static io.github.chenyilei2016.maintain.manager.service.impl.DirectoryServiceImpl.DATE_TIME_FORMATTER;

/**
 * @author chenyilei
 * @since 2025/08/06 15:03
 */
@Mapper
public interface DirectoryNodeAssembler {
    DirectoryNodeAssembler INSTANCE = Mappers.getMapper(DirectoryNodeAssembler.class);

    default ScriptNodeDTO convert2ScriptNodeDTO(ScriptVO vo) {
        if (null == vo) {
            return null;
        }

        ScriptNodeDTO dto = new ScriptNodeDTO();

        Script script = vo.getScript();
        DirectoryNode node = vo.getDirectoryNode();

        //赋值
        dto.setId(node.getId());
        dto.setName(node.getName());
        dto.setType(node.getType());
        dto.setParentId(node.getParentId());
        dto.setServiceName(node.getServiceName());
        dto.setContent(script.getContent());
        dto.setPermissions(script.getPermissions());
        dto.setCreator(node.getCreatorId());
        if (node.getCreateTime() != null) {
            dto.setCreateTime(node.getCreateTime().format(DATE_TIME_FORMATTER));
        }
        if (node.getUpdateTime() != null) {
            dto.setUpdateTime(node.getUpdateTime().format(DATE_TIME_FORMATTER));
        }
        dto.setPermissionType(node.getPermissionType());
        return dto;
    }

    default DirectoryNodeDTO convert2DirectoryNodeDTO(DirectoryNode node) {
        DirectoryNodeDTO dto = new DirectoryNodeDTO();
        BeanUtils.copyProperties(node, dto);

        if (node.getCreateTime() != null) {
            dto.setCreateTime(node.getCreateTime().format(DATE_TIME_FORMATTER));
        }
        if (node.getUpdateTime() != null) {
            dto.setUpdateTime(node.getUpdateTime().format(DATE_TIME_FORMATTER));
        }

        return dto;
    }
}
