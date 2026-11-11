package com.funnight.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * When the built web app is bundled into this server (see the Dockerfile), its client-side
 * routes must all load index.html so a refresh or a scanned QR link doesn't 404.
 */
@Controller
public class SpaController {

    @GetMapping({"/quick", "/join", "/join/{code}", "/room/{id}"})
    public String app() {
        return "forward:/index.html";
    }
}
