package io.github.chenyilei2016.maintain.manager.security;

import io.github.chenyilei2016.maintain.client.common.constants.MaintainConsoleClientCommonConst;
import io.github.chenyilei2016.maintain.client.common.dto.BaseSignDTO;
import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
public class RequestSigner {
    private final ManagerProperties.Security security;

    public RequestSigner(ManagerProperties managerProperties) {
        this.security = managerProperties.getSecurity();
    }

    public void sign(BaseSignDTO request, ServiceInstance instance) {
        if (clientProtocolVersion(instance) >= 2) {
            if (!StringUtils.hasText(security.getPrivateKey()) || !StringUtils.hasText(security.getKeyId())) {
                throw new IllegalStateException("未配置 RSA-SHA256 请求签名私钥");
            }
            request.grantSignV2(security.getKeyId(), UUID.randomUUID().toString(), security.getPrivateKey());
            return;
        }
        if (!security.isAllowLegacyClients() || !StringUtils.hasText(security.getLegacyPrivateKey())) {
            throw new IllegalStateException("目标是旧版客户端，已禁用或未配置旧版签名兼容");
        }
        request.grantSign(security.getLegacyPrivateKey());
        if (!StringUtils.hasText(request.getSign())) {
            throw new IllegalStateException("旧版请求签名失败");
        }
    }

    private int clientProtocolVersion(ServiceInstance instance) {
        String version = instance.getMetadata().get(MaintainConsoleClientCommonConst.KEY_REGISTRY_VERSION);
        try {
            return version == null ? 1 : Integer.parseInt(version);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }
}
