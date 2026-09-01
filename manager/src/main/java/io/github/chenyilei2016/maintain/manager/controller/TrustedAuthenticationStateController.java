package io.github.chenyilei2016.maintain.manager.controller;

import io.github.chenyilei2016.maintain.manager.controller.dto.res.AuthenticationStateWebResponse;
import io.github.chenyilei2016.maintain.manager.identity.AuthenticationProviderType;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/auth")
@ConditionalOnProperty(prefix = "maintain.manager.identity", name = "mode", havingValue = "TRUSTED_HEADERS")
public class TrustedAuthenticationStateController {
    @GetMapping("/state")
    public AjaxResult<AuthenticationStateWebResponse> state() {
        return AjaxResult.success(new AuthenticationStateWebResponse(true,
                AuthenticationProviderType.TRUSTED_HEADERS, ""));
    }
}
