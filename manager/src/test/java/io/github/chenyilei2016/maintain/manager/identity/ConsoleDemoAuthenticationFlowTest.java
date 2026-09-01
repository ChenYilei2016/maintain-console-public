package io.github.chenyilei2016.maintain.manager.identity;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.github.chenyilei2016.maintain.manager.MaintainManagerBootstrap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MaintainManagerBootstrap.class, properties = "maintain.manager.ai.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class ConsoleDemoAuthenticationFlowTest {
    @Autowired
    MockMvc mvc;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        Path path = Path.of(System.getProperty("java.io.tmpdir"), "maintain-demo-auth-" + UUID.randomUUID() + ".sqlite");
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + path);
    }

    @Test
    void demoUsesIndependentPasswordLoginWithoutEnumeratingAccounts() throws Exception {
        JSONObject response = JSON.parseObject(mvc.perform(get("/manager/auth/state"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertEquals("LOCAL_PASSWORD", response.getJSONObject("data").getString("provider"));
        org.junit.jupiter.api.Assertions.assertFalse(response.getJSONObject("data").containsKey("accounts"));
    }
}
