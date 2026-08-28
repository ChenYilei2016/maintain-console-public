package io.github.chenyilei2016.maintain.manager.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class IndexController {
    @GetMapping(value = {"/index.html", "/"})
    public String index() {
        return "forward:/static/console/index.html";
    }

    @GetMapping("/groovy")
    public ModelAndView groovy() {
        return new ModelAndView("index");
    }


}
