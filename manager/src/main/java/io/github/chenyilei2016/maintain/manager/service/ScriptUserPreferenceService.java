package io.github.chenyilei2016.maintain.manager.service;

import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ScriptUserPreferenceDO;
import io.github.chenyilei2016.maintain.manager.pojo.dto.ScriptResourceOverviewDTO;
import io.github.chenyilei2016.maintain.manager.pojo.mapper.ScriptUserPreferenceMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ScriptUserPreferenceService {
    private final ScriptUserPreferenceMapper preferenceMapper;

    public ScriptUserPreferenceService(ScriptUserPreferenceMapper preferenceMapper) {
        this.preferenceMapper = preferenceMapper;
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

    public void favorite(String userId, String scriptId, boolean favorite) {
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

    public ScriptResourceOverviewDTO overview(String userId, String serviceName) {
        ScriptResourceOverviewDTO overview = new ScriptResourceOverviewDTO();
        overview.setFavorites(preferenceMapper.selectFavorites(userId, serviceName));
        overview.setRecent(preferenceMapper.selectRecent(userId, serviceName));
        return overview;
    }

    private void insertOrRetry(ScriptUserPreferenceDO preference, boolean touch) {
        try {
            preferenceMapper.insert(preference);
        } catch (DuplicateKeyException race) {
            if (touch) {
                touch(preference.getUserId(), preference.getScriptId());
            } else {
                favorite(preference.getUserId(), preference.getScriptId(), Boolean.TRUE.equals(preference.getFavorite()));
            }
        }
    }
}
