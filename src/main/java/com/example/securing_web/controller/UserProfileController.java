package com.example.securing_web.controller;

import com.example.securing_web.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserProfileController {

    @GetMapping("/profile") // Измените путь на /profile
    public String userProfile(@AuthenticationPrincipal User currentUser , Model model) {
        model.addAttribute("currentUser ", currentUser );
        return "userProfile"; // Имя вашего шаблона
    }
}


