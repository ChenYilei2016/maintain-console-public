package io.github.chenyilei2016.maintain.manager.controller.manager;

import io.github.chenyilei2016.maintain.manager.constant.ConsoleRole;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxResult;
import io.github.chenyilei2016.maintain.manager.pojo.dto.UsageStatisticsDTO;
import io.github.chenyilei2016.maintain.manager.pojo.mapper.ScriptExecutionHistoryMapper;
import io.github.chenyilei2016.maintain.manager.security.RequireConsoleRole;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/manager/admin/usage")
@RequireConsoleRole(ConsoleRole.ADMIN)
@RequiredArgsConstructor
public class UsageStatisticsController {
    private static final int TOP_TOOL_LIMIT = 10;
    private final ScriptExecutionHistoryMapper histories;

    @GetMapping
    public AjaxResult<UsageStatisticsDTO> overview(@RequestParam(defaultValue = "MONTH") UsageWindow window) {
        LocalDateTime since = LocalDateTime.now().minusDays(window.days);
        return AjaxResult.success(new UsageStatisticsDTO(window.name(), window.days,
                histories.selectUsageSummary(since), histories.selectTopToolUsage(since, TOP_TOOL_LIMIT)));
    }

    @Getter
    public enum UsageWindow {
        WEEK(7),
        MONTH(30),
        QUARTER(90);

        private final int days;

        UsageWindow(int days) {
            this.days = days;
        }
    }
}
