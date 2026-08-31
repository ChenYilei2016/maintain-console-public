package io.github.chenyilei2016.maintain.manager.controller;

import io.github.chenyilei2016.maintain.manager.security.SecurityHeadersFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Controller
public class IndexController {
    /**
     * 为 CodeMirror 的动态样式提供单次页面 nonce，HTML 不可缓存复用。
     */
    @GetMapping(value = {"/", "/index.html", "/static/console/", "/static/console/index.html"},
            produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> index(HttpServletRequest request) throws IOException {
        String html = new ClassPathResource("static/console/index.html").getContentAsString(StandardCharsets.UTF_8);
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
