package io.github.chenyilei2016.maintain.manager.security;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.github.chenyilei2016.maintain.manager.MaintainManagerBootstrap;
import io.github.chenyilei2016.maintain.manager.config.ManagerProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MaintainManagerBootstrap.class, properties = {
        "maintain.manager.ai.enabled=false",
        "maintain.manager.security.identity-shared-secret=0123456789abcdef0123456789abcdef",
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.config.import-check.enabled=false",
        "spring.cloud.service-registry.auto-registration.enabled=false",
        "spring.flyway.locations=classpath:db/migration/sqlite"
})
@AutoConfigureMockMvc
@ActiveProfiles("trusted-test")
class TrustedAuthenticationFlowTest {
    @Autowired
    MockMvc mvc;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        Path path = Path.of(System.getProperty("java.io.tmpdir"), "maintain-trusted-auth-" + UUID.randomUUID() + ".sqlite");
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + path);
    }

    @Test
    void trustedIdentityCanInitializeTheReactApplicationWithoutMockLogin() throws Exception {
        JSONObject state = JSON.parseObject(mvc.perform(get("/manager/auth/state")
                        .headers(signed("GET", "/manager/auth/state", "nonce-state-0001")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertTrue(state.getJSONObject("data").getBooleanValue("authenticated"));
        assertEquals("TRUSTED_HEADERS", state.getJSONObject("data").getString("provider"));
        assertTrue(state.getJSONObject("data").getJSONArray("accounts").isEmpty());

        JSONObject login = JSON.parseObject(mvc.perform(post("/manager/login/getInfo")
                        .headers(signed("POST", "/manager/login/getInfo", "nonce-login-0001")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals("1001", login.getJSONObject("data").getString("userId"));
    }

    private HttpHeaders signed(String method, String uri, String nonce) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String roles = "DEVELOPER";
        String payload = String.join("\n", "1001", "Chen", roles, timestamp, nonce, method, uri);
        ManagerProperties.Security security = new ManagerProperties.Security();
        security.setIdentitySharedSecret("0123456789abcdef0123456789abcdef");
        String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(new TrustedIdentityVerifier(security).hmac(payload));
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Maintain-User-Id", "1001");
        headers.set("X-Maintain-User-Name", "Chen");
        headers.set("X-Maintain-User-Roles", roles);
        headers.set("X-Maintain-Identity-Timestamp", timestamp);
        headers.set("X-Maintain-Identity-Nonce", nonce);
        headers.set("X-Maintain-Identity-Signature", signature);
        return headers;
    }
}
