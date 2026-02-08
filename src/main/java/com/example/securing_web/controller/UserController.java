package com.example.securing_web.controller;

import com.example.securing_web.entity.User;
import com.example.securing_web.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Только админ может создавать пользователей
    @GetMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String showCreateUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roleTypes", new String[]{"ROLE_STUDENT", "ROLE_TEACHER", "ROLE_ADMIN"});
        return "admin/createUser";
    }

    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN')")
    public String createUser(@RequestParam String username,
                             @RequestParam String password,
                             @RequestParam String fullName,
                             @RequestParam String role,
                             RedirectAttributes redirectAttributes) {

        if(userService.findByUsername(username).isPresent()){
            redirectAttributes.addFlashAttribute("error", "Пользователь с таким именем уже существует");
            return "redirect:/users/create";
        }

        userService.register(username, password, fullName, role);
        redirectAttributes.addFlashAttribute("success", "Пользователь успешно создан");
        return "redirect:/admin/dashboard";
    }

    @GetMapping("/contact")
    @PreAuthorize("isAuthenticated()")
    public String contactPage(Model model) {
        List<User> users = userService.findAllUsers();
        model.addAttribute("users", users);
        return "contact";
    }
}