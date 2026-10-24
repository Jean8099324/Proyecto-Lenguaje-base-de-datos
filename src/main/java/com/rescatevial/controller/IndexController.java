package com.rescatevial.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @GetMapping("/")
    public String inicio() {
        return "auth/login";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard/index";
    }

}
