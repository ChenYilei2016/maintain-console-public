package io.github.chenyilei2016.maintain.manager.controller.manager;

import io.github.chenyilei2016.maintain.manager.context.LoginUserContext;
import io.github.chenyilei2016.maintain.manager.controller.dto.ExecutionApprovalCreateWebRequest;
import io.github.chenyilei2016.maintain.manager.controller.dto.ExecutionApprovalDecisionWebRequest;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxResult;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ExecutionApproval;
import io.github.chenyilei2016.maintain.manager.service.ExecutionApprovalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/manager/execution/approvals")
public class ExecutionApprovalController {
    private final ExecutionApprovalService approvalService;

    public ExecutionApprovalController(ExecutionApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping
    public AjaxResult<ExecutionApproval> create(@RequestBody @Valid ExecutionApprovalCreateWebRequest request) {
        return AjaxResult.success(approvalService.create(request, LoginUserContext.getUser()));
    }

    @GetMapping("/{approvalId}")
    public AjaxResult<ExecutionApproval> get(@PathVariable String approvalId) {
        return AjaxResult.success(approvalService.get(approvalId, LoginUserContext.getUser()));
    }

    @GetMapping("/mine")
    public AjaxResult<List<ExecutionApproval>> mine() {
        return AjaxResult.success(approvalService.listMine(LoginUserContext.getUser()));
    }

    @GetMapping("/pending")
    public AjaxResult<List<ExecutionApproval>> pending() {
        return AjaxResult.success(approvalService.listPending(LoginUserContext.getUser()));
    }

    @PostMapping("/{approvalId}/decision")
    public AjaxResult<ExecutionApproval> decide(
            @PathVariable String approvalId,
            @RequestBody @Valid ExecutionApprovalDecisionWebRequest request
    ) {
        return AjaxResult.success(approvalService.decide(approvalId, request, LoginUserContext.getUser()));
    }
}
