package io.github.chenyilei2016.maintain.manager.controller.dto.res;

import io.github.chenyilei2016.maintain.manager.identity.AuthenticationProviderType;

public record AuthenticationStateWebResponse(boolean authenticated,
                                             AuthenticationProviderType provider,
                                             String csrfToken) {
}
