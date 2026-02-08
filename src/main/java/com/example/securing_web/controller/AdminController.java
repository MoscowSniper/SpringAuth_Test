package com.example.securing_web.controller;

import com.example.securing_web.entity.User;
import com.example.securing_web.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        List<User> users = userService.findAllUsers();
        long studentCount = userService.countUsersByRole("ROLE_STUDENT");
        long teacherCount = userService.countUsersByRole("ROLE_TEACHER");
        long adminCount = userService.countUsersByRole("ROLE_ADMIN");

        model.addAttribute("totalUsers", users.size());
        model.addAttribute("studentCount", studentCount);
        model.addAttribute("teacherCount", teacherCount);
        model.addAttribute("adminCount", adminCount);
        model.addAttribute("recentUsers", users.size() > 5 ? users.subList(0, 5) : users);

        return "admin/dashboard";
    }

    @PostMapping("/users/{id}/roles")
    public String updateUserRoles(@PathVariable Long id,
                                  @RequestParam("roles") Set<String> roleNames,
                                  RedirectAttributes redirectAttributes) {
        try {
            userService.updateUserRoles(id, roleNames);
            redirectAttributes.addFlashAttribute("success", "Роли пользователя обновлены");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка обновления ролей: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails currentUser,
                             RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id, currentUser.getUsername());
            redirectAttributes.addFlashAttribute("success", "Пользователь удален");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления пользователя: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                @RequestParam String newPassword,
                                RedirectAttributes redirectAttributes) {
        try {
            if (newPassword.length() < 6) {
                throw new IllegalArgumentException("Пароль должен быть не менее 6 символов");
            }
            userService.changePassword(id, newPassword);
            redirectAttributes.addFlashAttribute("success", "Пароль сброшен");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка сброса пароля: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }
}