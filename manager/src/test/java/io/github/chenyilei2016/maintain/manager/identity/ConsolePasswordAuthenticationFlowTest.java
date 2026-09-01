package io.github.chenyilei2016.maintain.manager.identity;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.chenyilei2016.maintain.manager.MaintainManagerBootstrap;
import io.github.chenyilei2016.maintain.manager.pojo.dataobject.AuditLogDO;
import io.github.chenyilei2016.maintain.manager.pojo.mapper.AuditLogMapper;
import io.github.chenyilei2016.maintain.manager.pojo.mapper.ConsoleUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class ConsolePasswordAuthenticationFlowTest {
    private static final String ADMIN_PASSWORD = "Admin-password-2026";

    @Autowired
    MockMvc mvc;

    @Autowired
    ConsoleUserMapper users;

    @Autowired
    AuditLogMapper auditLogs;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        Path database = Path.of(System.getProperty("java.io.tmpdir"), "maintain-password-auth-" + UUID.randomUUID() + ".sqlite");
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + database);
        registry.add("maintain.manager.bootstrap-admin.password", () -> ADMIN_PASSWORD);
    }

    @Test
    void bootstrappedAdminCreatesAUserAndBothAuthenticateWithPasswords() throws Exception {
        Session anonymous = state();
        assertEquals("LOCAL_PASSWORD", anonymous.state().getString("provider"));
        assertFalse(anonymous.state().containsKey("accounts"), "登录状态不能枚举内置账号");
        assertFalse(login(anonymous, "admin", "wrong-password", "/admin").getBooleanValue("success"));

        JSONObject adminLogin = login(anonymous, "admin", ADMIN_PASSWORD, "/admin");
        assertTrue(adminLogin.getBooleanValue("success"), adminLogin.toJSONString());
        assertEquals("/admin", adminLogin.getString("data"));
        JSONObject adminInfo = postJson(anonymous, "/manager/login/getInfo", Map.of()).getJSONObject("data");
        assertEquals("admin", adminInfo.getString("employeeNo"));
        assertEquals("管理员", adminInfo.getString("employeeName"));
        assertTrue(adminInfo.getBooleanValue("administrator"));
        String administratorId = users.selectByEmployeeNo("admin").getId();
        JSONObject lastAdministratorUpdate = postJson(anonymous, "/manager/admin/users/" + administratorId,
                Map.of("status", "DISABLED", "roles", List.of()));
        assertFalse(lastAdministratorUpdate.getBooleanValue("success"));
        assertEquals(1, users.countActiveAdministrators());
        assertTrue(adminInfo.getJSONArray("availableEnvironments").stream().map(JSONObject.class::cast)
                        .anyMatch(environment -> "prod".equals(environment.getString("value"))
                                && environment.getBooleanValue("production")),
                "local profile must expose the SQLite-backed production simulation target");

        JSONObject created = postJson(anonymous, "/manager/admin/users", Map.of(
                "username", "developer", "displayName", "开发同学",
                "initialPassword", "Developer-password-2026", "roles", List.of()));
        assertTrue(created.getBooleanValue("success"), created.toJSONString());
        String developerId = created.getJSONObject("data").getString("id");
        assertTrue(postJson(anonymous, "/manager/auth/logout", Map.of()).getBooleanValue("success"));

        Session developer = state();
        JSONObject developerLogin = login(developer, "developer", "Developer-password-2026", "/workspace");
        assertTrue(developerLogin.getBooleanValue("success"), developerLogin.toJSONString());
        assertEquals("/workspace", developerLogin.getString("data"));
        JSONObject developerInfo = postJson(developer, "/manager/login/getInfo", Map.of()).getJSONObject("data");
        assertEquals("developer", developerInfo.getString("employeeNo"));
        assertFalse(developerInfo.getBooleanValue("administrator"));

        Session administrator = state();
        assertTrue(login(administrator, "admin", ADMIN_PASSWORD, "/admin").getBooleanValue("success"));
        assertTrue(postJson(administrator, "/manager/admin/users/" + developerId + "/password",
                Map.of("newPassword", "Developer-password-new-2026")).getBooleanValue("success"));
        Session nextLogin = state();
        assertFalse(login(nextLogin, "developer", "Developer-password-2026", "/workspace").getBooleanValue("success"));
        assertTrue(login(nextLogin, "developer", "Developer-password-new-2026", "/workspace").getBooleanValue("success"));
        assertTrue(postJson(administrator, "/manager/admin/users/" + developerId,
                Map.of("status", "DISABLED", "roles", List.of())).getBooleanValue("success"));
        assertEquals(1, auditCount("USER_CREATE"));
        assertEquals(1, auditCount("USER_PASSWORD_RESET"));
        assertEquals(1, auditCount("USER_UPDATE"));
        mvc.perform(post("/manager/login/getInfo").session(developer.session())
                        .header("X-CSRF-TOKEN", developer.csrf()))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/manager/auth/login").contentType("application/json")
                        .content(JSON.toJSONString(Map.of("username", "admin", "password", ADMIN_PASSWORD))))
                .andExpect(status().isForbidden());
    }

    private Session state() throws Exception {
        MvcResult result = mvc.perform(get("/manager/auth/state")).andExpect(status().isOk()).andReturn();
        JSONObject response = JSON.parseObject(result.getResponse().getContentAsString()).getJSONObject("data");
        return new Session((MockHttpSession) result.getRequest().getSession(false),
                response.getString("csrfToken"), response);
    }

    private JSONObject login(Session session, String username, String password, String returnTo) throws Exception {
        return postJson(session, "/manager/auth/login", Map.of(
                "username", username, "password", password, "returnTo", returnTo));
    }

    private JSONObject postJson(Session session, String path, Object body) throws Exception {
        return JSON.parseObject(mvc.perform(post(path).session(session.session()).header("X-CSRF-TOKEN", session.csrf())
                .contentType("application/json").content(JSON.toJSONString(body))).andReturn().getResponse().getContentAsString());
    }

    private long auditCount(String action) {
        return auditLogs.selectCount(Wrappers.<AuditLogDO>lambdaQuery().eq(AuditLogDO::getAction, action));
    }

    private record Session(MockHttpSession session, String csrf, JSONObject state) {
    }
}
