package io.github.chenyilei2016.maintain.manager.tools;

import io.github.chenyilei2016.maintain.manager.context.LoginUserContext;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxResult;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ServiceInstanceDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分享页只读取表单所需信息；授权使用独立的创建者／管理员入口。
 */
@RestController
@RequestMapping("/manager/tools")
@RequiredArgsConstructor
public class ToolController {
    private final ToolService tools;
    private final ToolCatalog catalog;

    @GetMapping
    public AjaxResult<ToolCatalog.ToolPage> list(@RequestParam(required = false) String serviceName,
                                                 @RequestParam(required = false) String search, @RequestParam(defaultValue = "ALL") ToolCatalog.View view,
                                                 @RequestParam(defaultValue = "0") int cursor) {
        if (search != null && search.length() > 200) throw new IllegalArgumentException("搜索文字过长");
        return AjaxResult.success(catalog.page(LoginUserContext.getUser(), serviceName, search, view, cursor));
    }

    @GetMapping("/{id}")
    public AjaxResult<ToolService.ToolForm> open(@PathVariable String id) {
        return AjaxResult.success(tools.open(id, LoginUserContext.getUser()));
    }

    @GetMapping("/{id}/instances")
    public AjaxResult<List<ServiceInstanceDTO>> instances(@PathVariable String id, @RequestParam String environment) {
        return AjaxResult.success(tools.instances(id, environment, LoginUserContext.getUser()));
    }

    @GetMapping("/{id}/grants")
    public AjaxResult<ToolService.GrantsView> grants(@PathVariable String id) {
        return AjaxResult.success(tools.grants(id, LoginUserContext.getUser()));
    }

    @PostMapping("/{id}/grants")
    public AjaxResult<Integer> updateGrants(@PathVariable String id, @RequestBody @Valid ToolService.GrantChange request) {
        return AjaxResult.success(tools.updateGrants(id, request, LoginUserContext.getUser()));
    }
}
