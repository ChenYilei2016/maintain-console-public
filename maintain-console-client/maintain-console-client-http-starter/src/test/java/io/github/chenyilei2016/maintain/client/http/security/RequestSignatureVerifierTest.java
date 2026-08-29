package io.github.chenyilei2016.maintain.client.http.security;

import io.github.chenyilei2016.maintain.client.common.dto.InvokeScriptParamSignDTO;
import io.github.chenyilei2016.maintain.client.common.utils.RSAUtil;
import io.github.chenyilei2016.maintain.client.http.properties.MaintainConsoleSecurityProperties;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.fail;

public class RequestSignatureVerifierTest {

    @Test
    public void verifiesV2SignatureAndRejectsReplay() {
        Map<String, String> keys = RSAUtil.generateKey();
        MaintainConsoleSecurityProperties properties = new MaintainConsoleSecurityProperties();
        properties.setAllowLegacySignatures(false);
        properties.setPublicKeys(Collections.singletonMap("test", keys.get("publicKey")));
        RequestSignatureVerifier verifier = new RequestSignatureVerifier(properties);
        InvokeScriptParamSignDTO request = new InvokeScriptParamSignDTO("return 1");
        request.grantSignV2("test", "0123456789abcdef", keys.get("privateKey"));

        verifier.verify(request);

        try {
            verifier.verify(request);
            fail("replayed request must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
