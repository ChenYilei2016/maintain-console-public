package io.github.chenyilei2016.maintain.manager.controller;

import io.github.chenyilei2016.maintain.manager.controller.dto.res.AuthenticationStateWebResponse;
import io.github.chenyilei2016.maintain.manager.identity.AuthenticationProviderType;
import io.github.chenyilei2016.maintain.manager.pojo.common.AjaxResult;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/manager/auth")
@Profile("!local & !demo")
public class TrustedAuthenticationStateController {
    @GetMapping("/state")
    public AjaxResult<AuthenticationStateWebResponse> state() {
        return AjaxResult.success(new AuthenticationStateWebResponse(true,
                AuthenticationProviderType.TRUSTED_HEADERS, "", List.of()));
    }
}
