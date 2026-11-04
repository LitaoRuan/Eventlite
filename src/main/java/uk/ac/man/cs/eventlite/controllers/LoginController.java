package uk.ac.man.cs.eventlite.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/sign-in")
    public String signIn() {
        return "security/sign-in"; 
    }
}