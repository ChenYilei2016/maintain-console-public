package io.github.chenyilei2016.maintain.manager.controller.manager;

import io.github.chenyilei2016.maintain.manager.context.LoginUserContext;
import io.github.chenyilei2016.maintain.manager.controller.dto.ExecutionTaskCreateWebRequest;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxResult;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ScriptExecutionTask;
import io.github.chenyilei2016.maintain.manager.pojo.entity.ServiceInstanceDTO;
import io.github.chenyilei2016.maintain.manager.service.ScriptExecutionTaskService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Validated
@RestController
@RequestMapping("/manager")
public class ScriptExecutionTaskController {
    private final ScriptExecutionTaskService executionTaskService;

    public ScriptExecutionTaskController(ScriptExecutionTaskService executionTaskService) {
        this.executionTaskService = executionTaskService;
    }

    @GetMapping("/service/instances")
    public AjaxResult<List<ServiceInstanceDTO>> listInstances(
            @RequestParam @NotBlank String serviceName,
            @RequestParam @NotBlank String environment
    ) {
        return AjaxResult.success(executionTaskService.listInstances(serviceName, environment));
    }

    @PostMapping("/script/tasks")
    public AjaxResult<ScriptExecutionTask> createTask(@RequestBody @Valid ExecutionTaskCreateWebRequest request) {
        return AjaxResult.success(executionTaskService.submit(request, LoginUserContext.getUser()));
    }

    @GetMapping("/script/tasks/{taskId}")
    public AjaxResult<ScriptExecutionTask> getTask(@PathVariable String taskId) {
        return AjaxResult.success(executionTaskService.getTask(taskId, LoginUserContext.getUser()));
    }

    @PostMapping("/script/tasks/{taskId}/cancel")
    public AjaxResult<ScriptExecutionTask> cancelTask(@PathVariable String taskId) {
        return AjaxResult.success(executionTaskService.cancel(taskId, LoginUserContext.getUser()));
    }

    @GetMapping(value = "/script/tasks/{taskId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter taskEvents(@PathVariable String taskId) {
        return executionTaskService.subscribe(taskId, LoginUserContext.getUser());
    }
}
