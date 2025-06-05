package com.example.securing_web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class UserController {

    private final UserService userService;

    // Конструктор с внедрением UserService
    public UserController(UserService userService) {
        this.userService = userService;
    }
    @GetMapping("/contact")
    public String contactPage(Model model) {
        List<User> users = userService.findAllUsers(); // твой сервис, который возвращает всех пользователей
        model.addAttribute("users", users);
        return "contact";
    }

}