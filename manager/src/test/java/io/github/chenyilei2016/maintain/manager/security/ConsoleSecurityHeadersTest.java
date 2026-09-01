package io.github.chenyilei2016.maintain.manager.security;

import io.github.chenyilei2016.maintain.manager.controller.IndexController;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class ConsoleSecurityHeadersTest {
    @Test
    void editorStylesUseFreshNonceOnEveryHtmlEntryPoint() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new IndexController())
                .addFilters(new SecurityHeadersFilter()).build();
        String previousNonce = null;
        for (String path : new String[]{"/index.html", "/static/console/index.html", "/static/console/",
                "/workspace", "/workspace/123", "/admin", "/admin/users", "/static/console/admin.html"}) {
            var response = mvc.perform(get(path)).andReturn().getResponse();
            assertEquals(200, response.getStatus());
            String policy = response.getHeader("Content-Security-Policy");
            var match = Pattern.compile("style-src 'self' 'nonce-([^']+)'").matcher(policy);
            assertTrue(match.find(), "CodeMirror dynamic styles need an authorized nonce");
            String nonce = match.group(1);
            assertNotEquals(previousNonce, nonce);
            assertTrue(response.getContentAsString().contains("name=\"csp-nonce\" content=\"" + nonce + "\""));
            assertEquals("no-store", response.getHeader("Cache-Control"));
            assertTrue(policy.contains("script-src 'self';"));
            assertFalse(policy.contains("unsafe-inline"));
            previousNonce = nonce;
        }
        var home = mvc.perform(get("/")).andReturn().getResponse();
        assertEquals(302, home.getStatus());
        assertEquals("/workspace", home.getRedirectedUrl());
        var legacyTool = mvc.perform(get("/tools/123")).andReturn().getResponse();
        assertEquals(302, legacyTool.getStatus());
        assertEquals("/workspace/123", legacyTool.getRedirectedUrl());

        String adminHtml = mvc.perform(get("/admin")).andReturn().getResponse().getContentAsString();
        String workspaceHtml = mvc.perform(get("/workspace")).andReturn().getResponse().getContentAsString();
        assertTrue(adminHtml.contains("Maintain Console 管理端"));
        assertFalse(adminHtml.equals(workspaceHtml), "管理端与脚本工作台必须使用不同 HTML/React 入口");
    }
}
