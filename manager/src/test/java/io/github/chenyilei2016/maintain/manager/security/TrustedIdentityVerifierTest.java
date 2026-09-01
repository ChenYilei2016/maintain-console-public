package io.github.chenyilei2016.maintain.manager.security;

import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class TrustedIdentityVerifierTest {

    @Test
    void verifiesSignedIdentityAndRejectsReplay() {
        ManagerProperties.Security security = new ManagerProperties.Security();
        security.setIdentitySharedSecret("0123456789abcdef0123456789abcdef");
        TrustedIdentityVerifier verifier = new TrustedIdentityVerifier(security);
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = "0123456789abcdef";
        String payload = String.join("\n", "1001", "Chen", "operator,admin", timestamp, nonce,
                "POST", "/manager/script/tasks");
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(verifier.hmac(payload));

        LocalLoginUser user = verifier.verify("1001", "Chen", "operator,admin", timestamp, nonce, signature,
                "POST", "/manager/script/tasks");

        assertEquals("1001", user.getId());
        assertEquals("1001", user.getEmployeeNo());
        assertTrue(user.getRoles().contains("ADMIN"));
        assertThrows(IllegalArgumentException.class, () -> verifier.verify(
                "1001", "Chen", "operator,admin", timestamp, nonce, signature,
                "POST", "/manager/script/tasks"));
    }
}
