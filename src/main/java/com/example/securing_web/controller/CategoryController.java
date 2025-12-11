package com.example.securing_web.controller;

import com.example.securing_web.entity.Category;
import com.example.securing_web.entity.Post;
import com.example.securing_web.service.CategoryService;
import com.example.securing_web.service.CommentService;
import com.example.securing_web.repository.PostRepository;
import org.springframework.data.domain.Pageable; // ПРАВИЛЬНЫЙ ИМПОРТ
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Controller
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final PostRepository postRepository;
    private final CommentService commentService;

    public CategoryController(CategoryService categoryService,
                              PostRepository postRepository,
                              CommentService commentService) {
        this.categoryService = categoryService;
        this.postRepository = postRepository;
        this.commentService = commentService;
    }

    @GetMapping
    public String showCategories(Model model) {
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        model.addAttribute("currentUsername", currentUsername);

        return "categories";
    }

    @GetMapping("/{categoryId}")
    public String showCategoryPosts(@PathVariable Long categoryId,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    Model model) {
        Category category = categoryService.getCategoryById(categoryId);

        // Получаем посты категории с пагинацией
        Pageable pageable = PageRequest.of(page, size);
        List<Post> posts = postRepository.findPostsByCategory(categoryId, pageable);

        // Загружаем комментарии для каждого поста
        for (Post post : posts) {
            List<com.example.securing_web.entity.Comment> comments = commentService.getCommentTreeByPostId(post.getId());
            post.setComments(comments != null ? comments : new java.util.ArrayList<>());
        }

        model.addAttribute("category", category);
        model.addAttribute("posts", posts);
        model.addAttribute("commentService", commentService);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        model.addAttribute("currentUsername", currentUsername);

        return "categoryPosts";
    }

    @GetMapping("/create")
    public String showCreateCategoryForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        model.addAttribute("currentUsername", currentUsername);
        return "createCategory";
    }

    @PostMapping("/create")
    public String createCategory(@RequestParam String name,
                                 @RequestParam(required = false) String description,
                                 Model model) {
        try {
            categoryService.createCategory(name, description);
            return "redirect:/categories?success=category_created";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = auth.getName();
            model.addAttribute("currentUsername", currentUsername);
            return "createCategory";
        }
    }
}