package io.github.chenyilei2016.maintain.manager.service;

import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ScriptUserPreferenceDO;
import io.github.chenyilei2016.maintain.manager.pojo.dto.ScriptResourceOverviewDTO;
import io.github.chenyilei2016.maintain.manager.pojo.mapper.ScriptUserPreferenceMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ScriptUserPreferenceService {
    private final ScriptUserPreferenceMapper preferenceMapper;
    private final ScriptAccessControl access;
    private final io.github.chenyilei2016.maintain.manager.tools.ToolCatalog catalog;

    public ScriptUserPreferenceService(ScriptUserPreferenceMapper preferenceMapper, ScriptAccessControl access,
                                       io.github.chenyilei2016.maintain.manager.tools.ToolCatalog catalog) {
        this.preferenceMapper = preferenceMapper;
        this.access = access;
        this.catalog = catalog;
    }

    public void touch(String userId, String scriptId) {
        LocalDateTime now = LocalDateTime.now();
        ScriptUserPreferenceDO preference = new ScriptUserPreferenceDO()
                .setUserId(userId)
                .setScriptId(scriptId)
                .setLastOpenTime(now)
                .setUpdateTime(now);
        int updated = preferenceMapper.touch(preference);
        if (updated == 0) {
            insertOrRetry(new ScriptUserPreferenceDO()
                    .setUserId(userId)
                    .setScriptId(scriptId)
                    .setFavorite(false)
                    .setLastOpenTime(now)
                    .setOpenCount(1)
                    .setUpdateTime(now), true);
        }
    }

    public void favorite(LocalLoginUser actor, String scriptId, boolean favorite) {
        String userId = actor.getEmployeeNo();
        access.requireVisible(scriptId, actor);
        LocalDateTime now = LocalDateTime.now();
        int updated = preferenceMapper.updateFavorite(new ScriptUserPreferenceDO()
                .setUserId(userId)
                .setScriptId(scriptId)
                .setFavorite(favorite)
                .setUpdateTime(now));
        if (updated == 0) {
            insertOrRetry(new ScriptUserPreferenceDO()
                    .setUserId(userId)
                    .setScriptId(scriptId)
                    .setFavorite(favorite)
                    .setOpenCount(0)
                    .setUpdateTime(now), false);
        }
    }

    public ScriptResourceOverviewDTO overview(LocalLoginUser actor, String serviceName) {
        String userId = actor.getEmployeeNo();
        ScriptResourceOverviewDTO overview = new ScriptResourceOverviewDTO();
        overview.setFavorites(shortcuts(catalog.page(actor, serviceName, null,
                io.github.chenyilei2016.maintain.manager.tools.ToolCatalog.View.FAVORITES, 0)));
        overview.setRecent(shortcuts(catalog.page(actor, serviceName, null,
                io.github.chenyilei2016.maintain.manager.tools.ToolCatalog.View.RECENT, 0)));
        return overview;
    }

    private java.util.List<io.github.chenyilei2016.maintain.manager.pojo.dto.ScriptShortcutDTO> shortcuts(
            io.github.chenyilei2016.maintain.manager.tools.ToolCatalog.ToolPage page) {
        return page.items().stream().map(item -> {
            var shortcut = new io.github.chenyilei2016.maintain.manager.pojo.dto.ScriptShortcutDTO();
            shortcut.setId(item.id());
            shortcut.setName(item.name());
            shortcut.setServiceName(item.serviceName());
            shortcut.setFavorite(item.favorite());
            shortcut.setLastOpenTime(item.lastOpenTime());
            return shortcut;
        }).toList();
    }

    private void insertOrRetry(ScriptUserPreferenceDO preference, boolean touch) {
        try {
            preferenceMapper.insert(preference);
        } catch (DuplicateKeyException race) {
            if (touch) {
                preferenceMapper.touch(preference);
            } else {
                preferenceMapper.updateFavorite(preference);
            }
        }
    }
}
