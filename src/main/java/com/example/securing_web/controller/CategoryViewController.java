package com.example.securing_web.controller;

import com.example.securing_web.entity.User;
import com.example.securing_web.repository.UserRepository;
import com.example.securing_web.service.CategoryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/categories")
public class CategoryViewController {

    private final CategoryService categoryService;
    private final UserRepository userRepository;

    public CategoryViewController(CategoryService categoryService,
                                  UserRepository userRepository) {
        this.categoryService = categoryService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String viewAllCategories(@AuthenticationPrincipal UserDetails userDetails,
                                    Model model) {
        String username = userDetails.getUsername();
        User user = userRepository.findByUsername(username).orElse(null);

        var accessibleCategories = categoryService.getCategoriesForUser(user);

        model.addAttribute("accessibleCategories", accessibleCategories);
        model.addAttribute("currentUser", user);

        return "categories";
    }
}
