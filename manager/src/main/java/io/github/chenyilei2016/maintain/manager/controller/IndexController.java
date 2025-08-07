package io.github.chenyilei2016.maintain.manager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
public class IndexController {
    @GetMapping(value = {"/index.html", "/"})
    public ModelAndView index(Map map) {
        return new ModelAndView("index-vue", map);
    }

    @GetMapping("/groovy")
    public ModelAndView groovy() {
        return new ModelAndView("index");
    }


}
