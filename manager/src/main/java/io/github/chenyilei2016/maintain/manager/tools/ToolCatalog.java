package io.github.chenyilei2016.maintain.manager.tools;

import io.github.chenyilei2016.maintain.manager.constant.ScriptPermissionEnum;
import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;
import io.github.chenyilei2016.maintain.manager.pojo.entity.Script;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptPermissionEntity;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptToolMetadata;
import io.github.chenyilei2016.maintain.manager.pojo.mapper.ToolCatalogMapper;
import io.github.chenyilei2016.maintain.manager.pojo.vo.ScriptVO;
import io.github.chenyilei2016.maintain.manager.service.ScriptAccessControl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ToolCatalog {
    private final ToolCatalogMapper mapper;
    private final ScriptAccessControl access;

    public ToolPage page(LocalLoginUser actor, String serviceName, String search, View view, int cursor) {
        if (cursor < 0 || cursor > 1_000_000) throw new IllegalArgumentException("目录游标超出范围，请重新搜索");
        String userId = actor.getEmployeeNo();
        var candidates = mapper.candidates(userId, serviceName,
                search == null || search.isBlank() ? null : "%" + search.trim() + "%", view.name(), cursor);
        List<Item> items = new ArrayList<>();
        int scanned = 0;
        for (var node : candidates) {
            if (scanned == 100 || items.size() == 20) break;
            scanned++;
            var tool = new ScriptVO().setDirectoryNode(node).setScript(new Script().setPermissions(node.getScriptPermissions()));
            if (!access.visible(tool, actor)) continue;
            boolean canRead = access.allows(tool, actor, ScriptPermissionEnum.READ);
            boolean canEdit = access.allows(tool, actor, ScriptPermissionEnum.EDIT);
            boolean canInvoke = access.allows(tool, actor, ScriptPermissionEnum.INVOKE);
            // “授权给我的”不包括 v1 的默认公开阅读。
            if (view == View.SHARED) {
                var grants = ScriptPermissionEntity.parse(node.getScriptPermissions());
                grants.setVersion(2);
                tool.getScript().setPermissions(com.alibaba.fastjson2.JSON.toJSONString(grants));
                if (!access.visible(tool, actor)) continue;
            }
            items.add(new Item(node.getId(), node.getName(), node.getDescription(), node.getServiceName(),
                    node.getCreatorId(), node.getCreatorName(), node.getVersion(),
                    ScriptToolMetadata.parse(node.getToolMetadata()), Boolean.TRUE.equals(node.getFavorite()),
                    node.getLastOpenTime(), canRead, canEdit, canInvoke));
        }
        return new ToolPage(List.copyOf(items), scanned < candidates.size() ? cursor + scanned : null);
    }

    public enum View {ALL, MINE, SHARED, FAVORITES, RECENT}

    public record ToolPage(List<Item> items, Integer nextCursor) {
    }

    public record Item(String id, String name, String description, String serviceName, String ownerId,
                       String owner, Integer version, ScriptToolMetadata metadata, boolean favorite,
                       LocalDateTime lastOpenTime, boolean canRead, boolean canEdit, boolean canInvoke) {
    }
}
