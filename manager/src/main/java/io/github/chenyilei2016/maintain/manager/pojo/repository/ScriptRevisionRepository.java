package io.github.chenyilei2016.maintain.manager.pojo.repository;

import io.github.chenyilei2016.maintain.manager.pojo.entity.Script;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptRevision;

import java.util.List;

public interface ScriptRevisionRepository {
    void saveRevision(Script script, String creatorId, String creatorName);

    List<ScriptRevision> listRecent(String scriptId, int limit);

    ScriptRevision findRevision(String scriptId, int version);
}
