package io.github.chenyilei2016.maintain.manager.security;

import io.github.chenyilei2016.maintain.client.common.constants.MaintainConsoleClientCommonConst;
import io.github.chenyilei2016.maintain.client.common.dto.InvokeScriptParamSignDTO;
import io.github.chenyilei2016.maintain.client.common.utils.RSAUtil;
import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.DefaultServiceInstance;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestSignerTest {

    @Test
    void signsV2RequestForCompatibleClient() {
        Map<String, String> keys = RSAUtil.generateKey();
        ManagerProperties properties = new ManagerProperties();
        properties.getSecurity().setKeyId("test");
        properties.getSecurity().setPrivateKey(keys.get("privateKey"));
        DefaultServiceInstance instance = new DefaultServiceInstance("node", "service", "127.0.0.1", 8080, false);
        instance.getMetadata().put(MaintainConsoleClientCommonConst.KEY_REGISTRY_VERSION, "2");
        InvokeScriptParamSignDTO request = new InvokeScriptParamSignDTO("return 1");

        new RequestSigner(properties).sign(request, instance);

        assertEquals(2, request.getSignVersion());
        assertTrue(RSAUtil.verifySha256(request.signaturePayloadV2(), request.getSign(), keys.get("publicKey")));
    }
}
