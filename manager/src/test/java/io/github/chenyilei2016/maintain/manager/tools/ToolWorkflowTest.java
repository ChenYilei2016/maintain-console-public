package io.github.chenyilei2016.maintain.manager.tools;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.github.chenyilei2016.maintain.client.common.console.IMaintainConsoleExecutor;
import io.github.chenyilei2016.maintain.client.common.dto.*;
import io.github.chenyilei2016.maintain.manager.MaintainManagerBootstrap;
import io.github.chenyilei2016.maintain.manager.caller.ClientCaller;
import io.github.chenyilei2016.maintain.manager.caller.ClientCallerContext;
import io.github.chenyilei2016.maintain.manager.constant.ConsoleRole;
import io.github.chenyilei2016.maintain.manager.context.LocalLoginUser;
import io.github.chenyilei2016.maintain.manager.context.LoginUserContext;
import io.github.chenyilei2016.maintain.manager.discovery.MaintainConsoleRegistryClientDiscovery;
import io.github.chenyilei2016.maintain.manager.service.ScriptInvoker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实 HTTP 业务入口 + 独立 SQLite；身份放入既有可信上下文，不增加生产鉴权入口。
 */
@SpringBootTest(classes = {MaintainManagerBootstrap.class, ToolWorkflowTest.ClientConfiguration.class},
        properties = {"maintain.manager.ai.enabled=false", "spring.datasource.hikari.maximum-pool-size=2",
                "maintain.manager.execution.max-targets=2"})
@ActiveProfiles("local")
class ToolWorkflowTest {
    private static final String SCHEMA = """
            {"version":1,"parameters":[{"name":"value","type":"STRING","required":true},
            {"name":"secret","type":"STRING","sensitive":true,"defaultValue":"sensitive-default"}]}
            """;
    private static final String CONTENT = "return [$${value}, $${secret}, _caller.employeeNo]";
    @Autowired
    WebApplicationContext context;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    RecordingClient client;
    @Autowired
    RecordingDiscovery discovery;
    private MockMvc mvc;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        Path database = Path.of(System.getProperty("java.io.tmpdir"), "maintain-tools-" + UUID.randomUUID() + ".sqlite");
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + database);
    }

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
        client.calls.set(0);
        client.delay = false;
        client.failPort = -1;
        client.missingResponse = false;
        discovery.count = 1;
    }

    @AfterEach
    void clearIdentity() {
        LoginUserContext.remove();
    }

    @Test
    void authorSharesRunnerUsesSavedVersionAndRevocationIsImmediate() throws Exception {
        String id = create();
        assertTrue(post("runner", "/manager/directory/treeNode/save", Map.of("nodeType", "script", "nodeName", "自己的脚本",
                        "serviceName", "maintain-console", "content", "return 999")).getBooleanValue("success"),
                "脚本创建不应依赖管理端角色，新脚本必须由 JSON ACL 保护");
        JSONObject lockedDetail = post("runner", "/manager/directory/script/detail", Map.of("scriptId", id));
        assertTrue(lockedDetail.getBooleanValue("success"));
        assertEquals("", lockedDetail.getJSONObject("data").getString("content"));
        assertFalse(lockedDetail.getJSONObject("data").getBooleanValue("canInvoke"));
        assertFalse(get("runner", "/manager/tools/" + id).getBooleanValue("success"));
        grant(id, 1);
        String form = get("runner", "/manager/tools/" + id).toJSONString();
        assertTrue(form.contains("\"success\":true"));
        assertFalse(form.contains(CONTENT));
        assertFalse(form.contains("readerNo"));
        assertFalse(form.contains("sensitive-default"));
        JSONObject request = runRequest(id, 2);
        request.put("script", "throw new RuntimeException('injected')");
        request.put("parameterSchema", "{\"version\":0}");
        request.put("service", "forged-service");
        JSONObject result = post("runner", "/manager/tools/run", request);
        assertEquals("SUCCESS", result.getJSONObject("data").getString("outcome"), result.toJSONString());
        assertEquals(1, client.calls.get());
        assertTrue(result.toJSONString().contains("runner"));
        assertFalse(result.toJSONString().contains("sensitive-default"));
        assertFalse(post("stranger", "/manager/tools/run", request).getBooleanValue("success"));
        assertFalse(get("stranger", "/manager/script/history?scriptId=" + id).getBooleanValue("success"));
        assertFalse(get("runner", "/manager/directory/script/revisions?scriptId=" + id).getBooleanValue("success"));
        assertFalse(post("runner", "/manager/scripts/debug", debugRequest(id, 2)).getBooleanValue("success"));
        assertFalse(post("runner", "/manager/script/eval/v2", Map.of("scriptId", id, "service", "maintain-console",
                "env", "random", "version", 2, "script", "return 999", "riskConfirmed", true)).getBooleanValue("success"));
        assertEquals(1, client.calls.get());
        JSONObject revoke = JSON.parseObject("""
                {"expectedVersion":2,"permissions":{"readerNo":"editor","editorNo":"editor","invokerNo":"editor",
                "allowedEnvironments":["random"],"enabled":true}}
                """);
        assertTrue(post("author", "/manager/tools/" + id + "/grants", revoke).getBooleanValue("success"));
        assertFalse(get("runner", "/manager/tools/" + id).getBooleanValue("success"));
        assertFalse(post("runner", "/manager/tools/run", runRequest(id, 3)).getBooleanValue("success"));
        assertEquals(1, client.calls.get());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM mc_script_execution_task", Integer.class));
    }

    @Test
    void directoryIsVisibleButAdministratorCannotBypassScriptJson() throws Exception {
        String id = create();
        String permissions = jdbc.queryForObject("SELECT permissions FROM mc_script WHERE id = ?", String.class, id);
        assertNotNull(permissions);
        JSONObject grants = JSON.parseObject(permissions);
        assertEquals(3, grants.getIntValue("version"));
        for (String field : List.of("readerNo", "editorNo", "invokerNo", "managerNo")) {
            assertEquals("author", grants.getString(field), field + " 必须显式包含创建者");
        }

        JSONObject strangerTree = post("stranger", "/manager/directory/tree?serviceName=maintain-console", Map.of());
        assertTrue(strangerTree.getBooleanValue("success"));
        assertTrue(strangerTree.toJSONString().contains(id), "目录名称对所有登录用户可见");
        JSONObject strangerDetail = post("stranger", "/manager/directory/script/detail", Map.of("scriptId", id));
        assertTrue(strangerDetail.getBooleanValue("success"));
        assertEquals("", strangerDetail.getJSONObject("data").getString("content"));
        assertFalse(strangerDetail.getJSONObject("data").getBooleanValue("canRead"));

        JSONObject administratorDetail = post("administrator", "/manager/directory/script/detail", Map.of("scriptId", id));
        assertTrue(administratorDetail.getBooleanValue("success"));
        assertEquals("", administratorDetail.getJSONObject("data").getString("content"), "管理角色不能读取脚本源码");
        assertFalse(administratorDetail.getJSONObject("data").getBooleanValue("canRead"));
        assertFalse(post("administrator", "/manager/tools/run", runRequest(id, 1)).getBooleanValue("success"),
                "管理角色不能运行脚本");
        assertFalse(get("administrator", "/manager/tools/" + id + "/grants").getBooleanValue("success"),
                "管理角色不能修改脚本授权");
    }

    @Test
    void editPermissionWorksWithoutASeparateReadGrant() throws Exception {
        String id = create();
        assertTrue(post("author", "/manager/tools/" + id + "/grants", Map.of("expectedVersion", 1,
                "permissions", Map.of("readerNo", "author", "editorNo", "author,editor",
                        "invokerNo", "author,editor", "managerNo", "author",
                        "allowedEnvironments", List.of("random"), "enabled", true))).getBooleanValue("success"));

        JSONObject detail = post("editor", "/manager/directory/script/detail", Map.of("scriptId", id));
        assertTrue(detail.getBooleanValue("success"));
        assertEquals(CONTENT, detail.getJSONObject("data").getString("content"));
        assertFalse(detail.getJSONObject("data").getBooleanValue("canRead"));
        assertTrue(detail.getJSONObject("data").getBooleanValue("canEdit"));
        assertTrue(get("editor", "/manager/service/instances?scriptId=" + id + "&environment=random")
                .getBooleanValue("success"));
        assertEquals("SUCCESS", post("editor", "/manager/scripts/debug", debugRequest(id, 2))
                .getJSONObject("data").getString("outcome"));
    }

    @Test
    void editorsSaveWithoutManagingGrantsAndConflictsNeverOverwrite() throws Exception {
        String id = create();
        grant(id, 1);
        JSONObject save = JSON.parseObject(JSON.toJSONString(Map.of("nodeId", id, "nodeType", "script", "nodeName", id,
                "serviceName", "maintain-console", "content", CONTENT + "\n// editor", "parameterSchema", SCHEMA,
                "expectedVersion", 2)));
        assertTrue(post("editor", "/manager/directory/treeNode/save", save).getBooleanValue("success"));
        assertFalse(post("editor", "/manager/directory/treeNode/save", save).getBooleanValue("success"));
        save.put("expectedVersion", 3);
        save.put("permissions", "{}");
        assertFalse(post("editor", "/manager/directory/treeNode/save", save).getBooleanValue("success"));
        assertFalse(get("editor", "/manager/tools/" + id + "/grants").getBooleanValue("success"));
        assertFalse(post("runner", "/manager/tools/run", runRequest(id, 2)).getBooleanValue("success"));
        assertTrue(post("editor", "/manager/directory/script/revision/restore",
                Map.of("scriptId", id, "version", 1, "expectedVersion", 3)).getBooleanValue("success"));
        assertTrue(get("runner", "/manager/tools/" + id).getBooleanValue("success"), "恢复版本不能恢复旧私有授权");
        assertTrue(post("runner", "/manager/tools/run", runRequest(id, 4)).getBooleanValue("success"));
        JSONObject editorHistory = get("editor", "/manager/script/history?scriptId=" + id);
        assertEquals(0, editorHistory.getJSONArray("data").size(), "源码可读不授予他人的查询结果");
        JSONObject ownerHistory = get("author", "/manager/script/history?scriptId=" + id);
        assertEquals(1, ownerHistory.getJSONArray("data").size());
        assertFalse(ownerHistory.toJSONString().contains(CONTENT));
    }

    @Test
    void permissionsFilterCatalogPreferencesTargetsAndLegacyEntrypoints() throws Exception {
        String id = create();
        grant(id, 1);
        assertTrue(get("runner", "/manager/tools?view=SHARED").toJSONString().contains(id));
        assertFalse(get("stranger", "/manager/tools?view=ALL").toJSONString().contains(id));
        assertTrue(post("runner", "/manager/resources/favorite", Map.of("scriptId", id, "favorite", true)).getBooleanValue("success"));
        assertTrue(get("runner", "/manager/resources/overview?serviceName=maintain-console").toJSONString().contains(id));
        JSONObject invalid = runRequest(id, 2);
        invalid.getJSONObject("target").put("selectionMode", "ALL");
        assertFalse(post("runner", "/manager/tools/run", invalid).getBooleanValue("success"));
        invalid.getJSONObject("target").put("environment", "forged");
        assertFalse(post("runner", "/manager/tools/run", invalid).getBooleanValue("success"));
        assertFalse(get("runner", "/manager/service/runtime-metadata?serviceName=maintain-console&environment=random&scriptId=" + id).getBooleanValue("success"));
        assertFalse(post("runner", "/devops/manager/script/eval", Map.of("scriptId", id, "service", "maintain-console",
                "env", "random", "params", "{}")).getBooleanValue("success"));
        assertEquals(0, client.calls.get());
    }

    @Test
    void timeoutIsUnknownAndNeverAutomaticallyRetried() throws Exception {
        String id = create();
        grant(id, 1);
        client.delay = true;
        JSONObject request = runRequest(id, 2);
        request.getJSONObject("target").put("timeoutSeconds", 1);
        JSONObject response = post("runner", "/manager/tools/run", request);
        assertEquals("UNKNOWN", response.getJSONObject("data").getString("outcome"), response.toJSONString());
        assertEquals(1, client.calls.get());
    }

    @Test
    void missingResponseIsUnknownNotAConfirmedBusinessFailure() throws Exception {
        String id = create();
        grant(id, 1);
        client.missingResponse = true;
        JSONObject report = post("runner", "/manager/tools/run", runRequest(id, 2)).getJSONObject("data");
        assertEquals("UNKNOWN", report.getString("outcome"), report.toJSONString());
        assertEquals(1, client.calls.get());
    }

    @Test
    void legacyRawTemplatesStayDeveloperOnlyAndRestoreCanClearSchema() throws Exception {
        JSONObject created = post("author", "/manager/directory/treeNode/save", Map.of(
                "nodeType", "script", "nodeName", "legacy", "serviceName", "maintain-console",
                "content", "return '$${value}'", "allowedEnvironments", List.of("random")));
        assertTrue(created.getBooleanValue("success"), created.toJSONString());
        String id = created.getString("data");
        assertFalse(post("author", "/manager/tools/" + id + "/grants", Map.of("expectedVersion", 1,
                "permissions", Map.of("invokerNo", "runner", "allowedEnvironments", List.of("random")))).getBooleanValue("success"));
        assertFalse(post("author", "/manager/tools/run", runRequest(id, 1)).getBooleanValue("success"));
        assertFalse(post("author", "/devops/manager/script/eval", Map.of("scriptId", id, "version", 1,
                "env", "random", "service", "maintain-console", "params", "{}", "riskConfirmed", true)).getBooleanValue("success"));
        JSONObject debug = debugRequest(id, 1);
        debug.put("content", "return '$${value}'");
        debug.put("parameterSchema", null);
        debug.put("parameters", Map.of("value", "compatibility"));
        assertEquals("SUCCESS", post("author", "/manager/scripts/debug", debug).getJSONObject("data").getString("outcome"));
        assertTrue(post("author", "/manager/directory/treeNode/save", Map.of("nodeId", id,
                "nodeType", "script", "nodeName", "migrated", "serviceName", "maintain-console", "expectedVersion", 1,
                "content", CONTENT, "parameterSchema", SCHEMA)).getBooleanValue("success"));
        assertTrue(post("author", "/manager/directory/script/revision/restore",
                Map.of("scriptId", id, "version", 1, "expectedVersion", 2)).getBooleanValue("success"));
        assertNull(jdbc.queryForObject("SELECT parameter_schema FROM mc_script WHERE id = ?", String.class, id));
        assertFalse(post("author", "/manager/tools/run", runRequest(id, 3)).getBooleanValue("success"));
        assertEquals(1, client.calls.get());
    }

    @Test
    void multiInstanceExecutionEnforcesLimitAndReturnsPerTargetOutcomes() throws Exception {
        String id = create();
        assertTrue(post("author", "/manager/tools/" + id + "/grants", Map.of("expectedVersion", 1,
                "permissions", Map.of("invokerNo", "runner", "allowedEnvironments", List.of("random"),
                        "allowAllInstances", true))).getBooleanValue("success"));
        JSONObject request = runRequest(id, 2);
        request.getJSONObject("target").put("selectionMode", "ALL");
        discovery.count = 3;
        assertFalse(post("runner", "/manager/tools/run", request).getBooleanValue("success"));
        assertEquals(0, client.calls.get());
        discovery.count = 2;
        client.failPort = 10001;
        JSONObject report = post("runner", "/manager/tools/run", request).getJSONObject("data");
        assertEquals("PARTIAL_SUCCESS", report.getString("outcome"), report.toJSONString());
        assertEquals(2, report.getJSONArray("targets").size());
        assertEquals(2, client.calls.get(), "每个目标只调用一次");
        assertFalse(report.toJSONString().contains("private-backend-detail"));
    }

    private String create() throws Exception {
        JSONObject response = post("author", "/manager/directory/treeNode/save", Map.of(
                "nodeType", "script", "nodeName", "tool-" + UUID.randomUUID(), "serviceName", "maintain-console",
                "content", CONTENT, "parameterSchema", SCHEMA, "allowedEnvironments", java.util.List.of("random")));
        assertTrue(response.getBooleanValue("success"), response.toJSONString());
        return response.getString("data");
    }

    private void grant(String id, int version) throws Exception {
        JSONObject response = post("author", "/manager/tools/" + id + "/grants", Map.of("expectedVersion", version,
                "permissions", Map.of("readerNo", "author,editor", "editorNo", "author,editor",
                        "invokerNo", "author,runner,editor", "managerNo", "author",
                        "allowedEnvironments", java.util.List.of("random"), "enabled", true)));
        assertTrue(response.getBooleanValue("success"), response.toJSONString());
    }

    private JSONObject runRequest(String id, int version) {
        return JSON.parseObject(JSON.toJSONString(Map.of("scriptId", id, "version", version,
                "parameters", Map.of("value", "data"), "riskConfirmed", true,
                "target", Map.of("environment", "random", "selectionMode", "RANDOM", "timeoutSeconds", 5))));
    }

    private JSONObject debugRequest(String id, int version) {
        JSONObject request = runRequest(id, version);
        request.put("content", "return 'draft'");
        request.put("parameterSchema", "{\"version\":1,\"parameters\":[]}");
        request.put("parameters", Map.of());
        return request;
    }

    private JSONObject post(String actor, String path, Object body) throws Exception {
        identity(actor);
        return JSON.parseObject(mvc.perform(MockMvcRequestBuilders.post(path).contentType("application/json")
                .content(JSON.toJSONString(body))).andReturn().getResponse().getContentAsString());
    }

    private JSONObject get(String actor, String path) throws Exception {
        identity(actor);
        return JSON.parseObject(mvc.perform(MockMvcRequestBuilders.get(path)).andReturn().getResponse().getContentAsString());
    }

    private void identity(String id) {
        LocalLoginUser actor = new LocalLoginUser();
        actor.setEmployeeNo(id);
        actor.setEmployeeName("name-" + id);
        if (id.equals("administrator")) actor.getRoles().add(ConsoleRole.ADMIN.name());
        LoginUserContext.setUser(actor);
    }

    @TestConfiguration
    static class ClientConfiguration {
        @Bean
        @Primary
        RecordingDiscovery recordingDiscovery() {
            return new RecordingDiscovery();
        }

        @Bean
        @Primary
        RecordingClient recordingClient() {
            return new RecordingClient();
        }

        @Bean
        @Primary
        ScriptInvoker testScriptInvoker(RecordingClient client, ObjectProvider<IMaintainConsoleExecutor> local) {
            MockEnvironment environment = new MockEnvironment();
            environment.setActiveProfiles("test");
            return new ScriptInvoker(client, local, environment);
        }
    }

    static class RecordingClient implements ClientCaller {
        final AtomicInteger calls = new AtomicInteger();
        volatile boolean delay;
        volatile int failPort;
        volatile boolean missingResponse;

        @Override
        public ApiResult<InvokeScriptResultDTO> $invokeScript(ClientCallerContext context, InvokeScriptParamSignDTO request) {
            calls.incrementAndGet();
            if (missingResponse) return null;
            if (context.getServiceInstance().getPort() == failPort) return ApiResult.error("private-backend-detail");
            if (delay) {
                try {
                    Thread.sleep(2_000);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            InvokeScriptResultDTO result = new InvokeScriptResultDTO();
            result.setScriptResult(JSON.toJSONString(new groovy.lang.GroovyShell().evaluate(request.getScript())));
            return ApiResult.success(result);
        }

        @Override
        public ApiResult<InvokeCommandResultDTO> $invokeCommend(ClientCallerContext context, InvokeCommandParamSignDTO request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ApiResult<RuntimeMetadataDTO> $runtimeMetadata(ClientCallerContext context, RuntimeMetadataParamSignDTO request) {
            throw new UnsupportedOperationException();
        }
    }

    static class RecordingDiscovery implements MaintainConsoleRegistryClientDiscovery {
        int count = 1;

        @Override
        public ServiceInstance findServiceInstance(String service, String environment) {
            return listServiceInstances(service, environment).getFirst();
        }

        @Override
        public List<ServiceInstance> listServiceInstances(String service, String environment) {
            return IntStream.range(0, count).mapToObj(index -> (ServiceInstance) new DefaultServiceInstance(
                    "test-" + index, service, "127.0.0.1", 10000 + index, false)).toList();
        }

        @Override
        public List<String> listServiceNames() {
            return List.of("maintain-console");
        }
    }
}
