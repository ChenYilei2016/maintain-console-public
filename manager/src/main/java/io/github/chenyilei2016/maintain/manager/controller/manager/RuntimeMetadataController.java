package io.github.chenyilei2016.maintain.manager.controller.manager;

import io.github.chenyilei2016.maintain.client.common.dto.RuntimeMetadataDTO;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxResult;
import io.github.chenyilei2016.maintain.manager.service.RuntimeMetadataService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/manager/service")
public class RuntimeMetadataController {
    private final RuntimeMetadataService runtimeMetadataService;

    public RuntimeMetadataController(RuntimeMetadataService runtimeMetadataService) {
        this.runtimeMetadataService = runtimeMetadataService;
    }

    @GetMapping("/runtime-metadata")
    public AjaxResult<RuntimeMetadataDTO> runtimeMetadata(
            @RequestParam @NotBlank String serviceName,
            @RequestParam @NotBlank String environment,
            @RequestParam(required = false) String instanceId
    ) {
        return AjaxResult.success(runtimeMetadataService.load(serviceName, environment, instanceId));
    }
}
