package com.example.securing_web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String loginForm(@RequestParam(value="error", required=false) String error,
                            Model model){
        if(error != null){
            model.addAttribute("error", "Неверное имя пользователя или пароль");
        }
        return "login";
    }

    @GetMapping("/")
    public String hello(){
        return "hello";
    }
}