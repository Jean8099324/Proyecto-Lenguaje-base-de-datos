package com.rescatevial.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/error")
public class AccessDeniedController {

    // El forward de Spring Security conserva el método del envío rechazado.
    @RequestMapping("/403")
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN)
    public String forbidden() {
        return "error/403";
    }
}
