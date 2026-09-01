package io.github.chenyilei2016.maintain.manager.identity;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.github.chenyilei2016.maintain.manager.MaintainManagerBootstrap;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MaintainManagerBootstrap.class, properties = "maintain.manager.ai.enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ConsoleAuthenticationFlowTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    JdbcTemplate jdbc;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        Path path = Path.of(System.getProperty("java.io.tmpdir"), "maintain-auth-" + UUID.randomUUID() + ".sqlite");
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + path);
    }

    @Test
    void mockAccountsUseRealSessionUserStatusCsrfAndServerPermissions() throws Exception {
        Session developer = state();
        mvc.perform(post("/manager/auth/login").session(developer.session()).header("X-CSRF-TOKEN", developer.csrf())
                        .contentType("application/json").content(JSON.toJSONString(Map.of(
                                "accountId", "developer", "returnTo", "/workspace/example"))))
                .andExpect(status().isOk());
        JSONObject developerInfo = postJson(developer, "/manager/login/getInfo", Map.of());
        assertEquals("developer", developerInfo.getJSONObject("data").getString("employeeNo"));
        assertTrue(developerInfo.getJSONObject("data").getBooleanValue("canCreateTools"));
        assertFalse(developerInfo.getJSONObject("data").getBooleanValue("administrator"));
        assertFalse(getJson(developer, "/manager/admin/users").getBooleanValue("success"));
        assertFalse(getJson(developer, "/manager/admin/usage").getBooleanValue("success"));
        JSONObject created = postJson(developer, "/manager/directory/treeNode/save", Map.of(
                "nodeType", "script", "nodeName", "登录链路工具", "serviceName", "maintain-console",
                "content", "return 1", "parameterSchema", "{\"version\":1,\"parameters\":[]}",
                "allowedEnvironments", List.of("random")));
        assertTrue(created.getBooleanValue("success"), created.toJSONString());
        String scriptId = created.getString("data");

        Session admin = login("admin", "/admin");
        JSONObject users = getJson(admin, "/manager/admin/users");
        assertTrue(users.getBooleanValue("success"), users.toJSONString());
        String developerId = jdbc.queryForObject("SELECT id FROM mc_console_user WHERE employee_no = 'developer'", String.class);
        JSONObject disabled = postJson(admin, "/manager/admin/users/" + developerId,
                Map.of("status", "DISABLED", "roles", List.of("DEVELOPER")));
        assertTrue(disabled.getBooleanValue("success"), disabled.toJSONString());
        assertTrue(postJson(admin, "/manager/directory/script/detail", Map.of("scriptId", scriptId)).getBooleanValue("success"));
        assertTrue(postJson(admin, "/manager/directory/treeNode/save", Map.of(
                "nodeId", scriptId, "nodeType", "script", "nodeName", "登录链路工具", "serviceName", "maintain-console",
                "expectedVersion", 1, "content", "return 2", "parameterSchema", "{\"version\":1,\"parameters\":[]}"))
                .getBooleanValue("success"), "系统管理员可以按同一版本锁编辑，不绕过保存校验");
        assertTrue(postJson(admin, "/manager/tools/" + scriptId + "/grants", Map.of("expectedVersion", 2,
                "permissions", Map.of("invokerNo", "operator", "allowedEnvironments", List.of("random"), "enabled", true)))
                .getBooleanValue("success"));
        mvc.perform(post("/manager/login/getInfo").session(developer.session()).header("X-CSRF-TOKEN", developer.csrf()))
                .andExpect(status().isUnauthorized());

        Session operator = login("operator", "/tools/tool-id");
        JSONObject operatorInfo = postJson(operator, "/manager/login/getInfo", Map.of());
        assertFalse(operatorInfo.getJSONObject("data").getBooleanValue("canCreateTools"));
        assertFalse(postJson(operator, "/manager/directory/script/detail", Map.of("scriptId", scriptId)).getBooleanValue("success"));
        assertTrue(getJson(operator, "/manager/tools/" + scriptId).getBooleanValue("success"));
        JSONObject executed = postJson(operator, "/manager/tools/run", Map.of(
                "scriptId", scriptId, "version", 3, "parameters", Map.of(), "riskConfirmed", true,
                "target", Map.of("environment", "random", "selectionMode", "RANDOM", "timeoutSeconds", 5)));
        assertEquals("SUCCESS", executed.getJSONObject("data").getString("outcome"), executed.toJSONString());
        JSONObject usage = getJson(admin, "/manager/admin/usage?window=WEEK");
        assertTrue(usage.getBooleanValue("success"), usage.toJSONString());
        assertTrue(usage.getJSONObject("data").getJSONObject("summary").getLongValue("totalExecutions") >= 1);
        assertEquals(scriptId, usage.getJSONObject("data").getJSONArray("tools").getJSONObject(0).getString("scriptId"));
        JSONObject create = postJson(operator, "/manager/directory/treeNode/save", Map.of(
                "nodeType", "script", "nodeName", "越权脚本", "serviceName", "maintain-console",
                "content", "return 1"));
        assertFalse(create.getBooleanValue("success"));

        mvc.perform(post("/manager/auth/login").contentType("application/json")
                        .content("{\"accountId\":\"admin\"}"))
                .andExpect(status().isForbidden());
        Session invalid = state();
        assertFalse(postJson(invalid, "/manager/auth/login", Map.of("accountId", "root", "returnTo", "/"))
                .getBooleanValue("success"));
        assertFalse(postJson(invalid, "/manager/auth/login", Map.of("accountId", "admin", "returnTo", "//evil.test"))
                .getBooleanValue("success"));
        mvc.perform(post("/manager/login/getInfo").cookie(new Cookie("JSESSIONID", "forged"))
                        .header("X-Maintain-User-Id", "admin"))
                .andExpect(status().isUnauthorized());

        assertTrue(postJson(admin, "/manager/auth/logout", Map.of()).getBooleanValue("success"));
        mvc.perform(post("/manager/login/getInfo")).andExpect(status().isUnauthorized());
        Session defaultAdmin = state();
        assertEquals("/admin", postJson(defaultAdmin, "/manager/auth/login", Map.of("accountId", "admin", "returnTo", "/"))
                .getString("data"));
    }

    private Session login(String account, String returnTo) throws Exception {
        Session state = state();
        JSONObject response = postJson(state, "/manager/auth/login", Map.of("accountId", account, "returnTo", returnTo));
        assertTrue(response.getBooleanValue("success"), response.toJSONString());
        assertEquals(returnTo, response.getString("data"));
        return state;
    }

    private Session state() throws Exception {
        MvcResult result = mvc.perform(get("/manager/auth/state")).andExpect(status().isOk()).andReturn();
        JSONObject response = JSON.parseObject(result.getResponse().getContentAsString());
        assertEquals(3, response.getJSONObject("data").getJSONArray("accounts").size());
        return new Session((MockHttpSession) result.getRequest().getSession(false),
                response.getJSONObject("data").getString("csrfToken"));
    }

    private JSONObject postJson(Session session, String path, Object body) throws Exception {
        return JSON.parseObject(mvc.perform(post(path).session(session.session()).header("X-CSRF-TOKEN", session.csrf())
                .contentType("application/json").content(JSON.toJSONString(body))).andReturn().getResponse().getContentAsString());
    }

    private JSONObject getJson(Session session, String path) throws Exception {
        return JSON.parseObject(mvc.perform(get(path).session(session.session())).andReturn().getResponse().getContentAsString());
    }

    private record Session(MockHttpSession session, String csrf) {
    }
}
