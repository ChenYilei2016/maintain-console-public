package io.github.chenyilei2016.maintain.manager.controller;

import io.github.chenyilei2016.maintain.manager.security.SecurityHeadersFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
public class IndexController {
    /**
     * 为 CodeMirror 的动态样式提供单次页面 nonce，HTML 不可缓存复用。
     */
    @GetMapping("/")
    public ResponseEntity<Void> home() {
        return ResponseEntity.status(302).location(java.net.URI.create("/workspace")).build();
    }

    @GetMapping("/tools/{id}")
    public ResponseEntity<Void> legacyToolLink(@PathVariable String id) {
        return ResponseEntity.status(302).location(UriComponentsBuilder.fromPath("/workspace/{id}")
                .buildAndExpand(id).encode().toUri()).build();
    }

    @GetMapping(value = {"/index.html", "/static/console/", "/static/console/index.html", "/login",
            "/workspace", "/workspace/{id}"},
            produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> workspace(HttpServletRequest request) throws IOException {
        return page("static/console/index.html", request);
    }

    @GetMapping(value = {"/admin", "/admin/{page}", "/static/console/admin.html"},
            produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> admin(HttpServletRequest request) throws IOException {
        return page("static/console/admin.html", request);
    }

    private ResponseEntity<String> page(String resource, HttpServletRequest request) throws IOException {
        String html = new ClassPathResource(resource).getContentAsString(StandardCharsets.UTF_8);
        String nonce = (String) request.getAttribute(SecurityHeadersFilter.CSP_NONCE_ATTRIBUTE);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html.replace("__CSP_NONCE__", nonce));
    }

    @GetMapping("/groovy")
    public ModelAndView groovy() {
        return new ModelAndView("index");
    }


}
