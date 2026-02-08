package com.example.securing_web.controller;

import com.example.securing_web.entity.Category;
import com.example.securing_web.entity.StudentGroup;
import com.example.securing_web.entity.User;
import com.example.securing_web.service.CategoryService;
import com.example.securing_web.service.GroupService;
import com.example.securing_web.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
public class CategoryController {

    private final CategoryService categoryService;
    private final UserService userService;
    private final GroupService groupService;

    public CategoryController(CategoryService categoryService,
                              UserService userService,
                              GroupService groupService) {
        this.categoryService = categoryService;
        this.userService = userService;
        this.groupService = groupService;
    }

    @GetMapping
    public String listCategories(Model model) {
        List<Category> categories = categoryService.getCategoriesWithGroups();
        List<User> teachers = userService.findUsersByRole("ROLE_TEACHER");
        List<StudentGroup> allGroups = groupService.findAllGroups();

        model.addAttribute("categories", categories);
        model.addAttribute("teachers", teachers);
        model.addAttribute("allGroups", allGroups);
        model.addAttribute("totalCategories", categoryService.getTotalCategoriesCount());

        return "admin/categories";
    }

    @PostMapping("/create")
    public String createCategory(@RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(defaultValue = "true") boolean visibleToAll,
                                 @RequestParam(defaultValue = "true") boolean studentsCanCreatePosts,
                                 @RequestParam(required = false) Long teacherId,
                                 @RequestParam(value = "allowedGroupIds", required = false) Set<Long> allowedGroupIds,
                                 RedirectAttributes redirectAttributes) {
        try {
            categoryService.createCategory(name, description, visibleToAll,
                    studentsCanCreatePosts, teacherId, allowedGroupIds);
            redirectAttributes.addFlashAttribute("success", "Раздел создан успешно");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка создания раздела: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/update")
    public String updateCategory(@PathVariable Long id,
                                 @RequestParam(required = false) String name,
                                 @RequestParam(required = false) String description,
                                 @RequestParam(required = false) Boolean visibleToAll,
                                 @RequestParam(required = false) Boolean studentsCanCreatePosts,
                                 @RequestParam(required = false) Long teacherId,
                                 @RequestParam(value = "allowedGroupIds", required = false) Set<Long> allowedGroupIds,
                                 RedirectAttributes redirectAttributes) {
        try {
            categoryService.updateCategory(id, name, description, visibleToAll,
                    studentsCanCreatePosts, teacherId, allowedGroupIds);
            redirectAttributes.addFlashAttribute("success", "Раздел обновлен успешно");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка обновления раздела: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/{id}/delete")
    public String deleteCategory(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("success", "Раздел удален успешно");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Ошибка удаления раздела: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @GetMapping("/{id}")
    public String viewCategory(@PathVariable Long id, Model model) {
        Category category = categoryService.findCategoryById(id)
                .orElseThrow(() -> new IllegalArgumentException("Раздел не найден"));

        model.addAttribute("category", category);
        model.addAttribute("allowedGroups", category.getAllowedGroups());

        return "admin/category-detail";
    }
}
