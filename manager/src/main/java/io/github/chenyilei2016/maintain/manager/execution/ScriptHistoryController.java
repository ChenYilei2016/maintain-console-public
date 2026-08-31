package io.github.chenyilei2016.maintain.manager.execution;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.chenyilei2016.maintain.manager.constant.ScriptPermissionEnum;
import io.github.chenyilei2016.maintain.manager.context.LoginUserContext;
import io.github.chenyilei2016.maintain.manager.exceptions.CommonException;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxPageResult;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.ScriptExecutionHistoryDO;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptExecutionHistoryEntity;
import io.github.chenyilei2016.maintain.manager.pojo.repository.ScriptExecutionHistoryRepository;
import io.github.chenyilei2016.maintain.manager.service.ScriptAccessControl;
import io.github.chenyilei2016.maintain.manager.service.ScriptContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 历史仅供查阅事实；读代码权限不授予他人的结果读取权限。
 */
@RestController
@RequiredArgsConstructor
public class ScriptHistoryController {
    private final ScriptExecutionHistoryRepository histories;
    private final ScriptContentService scripts;
    private final ScriptAccessControl access;

    @GetMapping("/manager/script/history")
    public AjaxPageResult<List<ScriptExecutionHistoryEntity>> history(@RequestParam String scriptId,
                                                                      @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        var user = LoginUserContext.getUser();
        var script = scripts.findById(scriptId);
        if (!access.visible(script, user.getEmployeeNo()))
            throw CommonException.createReminderException("无权查看此工具的历史");
        int boundedPage = Math.max(1, page);
        int boundedSize = Math.max(1, Math.min(size, 50));
        QueryWrapper<ScriptExecutionHistoryDO> query = new QueryWrapper<>();
        query.eq("script_id", scriptId);
        if (!access.allows(script, user.getEmployeeNo(), ScriptPermissionEnum.MANAGE)) {
            query.eq("executor_id", user.getEmployeeNo());
        }
        query.orderByDesc("id");
        var result = histories.page(new Page<>(boundedPage, boundedSize), query);
        result.getRecords().forEach(history -> {
            history.setScriptContent(null);
            history.setFinalScriptContent(null);
        });
        return new AjaxPageResult<>(true, result.getRecords(), null, boundedPage, boundedSize, result.getTotal());
    }
}
