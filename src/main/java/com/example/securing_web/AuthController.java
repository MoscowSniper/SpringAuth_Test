package com.example.securing_web;


import com.example.securing_web.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService){
        this.userService = userService;
    }

    @GetMapping("/register")
    public String registerForm(){
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@RequestParam String username,
                               @RequestParam String password,
                               Model model){
        if(!userService.register(username, password)){
            model.addAttribute("error", "Пользователь уже существует");
            return "register";
        }
        model.addAttribute("message", "Регистрация успешна! Войдите, пожалуйста.");
        return "login";
    }

    @GetMapping("/login")
    public String loginForm(@RequestParam(value="error", required=false) String error,
                            Model model){
        if(error != null){
            model.addAttribute("error", "Неверное имя пользователя или пароль");
        }
        return "login";
    }

    @GetMapping("/")
    public String home(){
        return "home"; // страница после входа
    }
}
