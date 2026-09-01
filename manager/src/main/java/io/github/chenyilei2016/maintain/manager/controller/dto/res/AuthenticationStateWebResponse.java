package io.github.chenyilei2016.maintain.manager.controller.dto.res;

import io.github.chenyilei2016.maintain.manager.identity.AuthenticationProviderType;
import io.github.chenyilei2016.maintain.manager.identity.MockLoginAccount;

import java.util.List;

public record AuthenticationStateWebResponse(boolean authenticated,
                                             AuthenticationProviderType provider,
                                             String csrfToken,
                                             List<MockLoginAccount.Option> accounts) {
}
