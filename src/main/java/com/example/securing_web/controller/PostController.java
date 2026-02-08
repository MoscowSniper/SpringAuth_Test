package com.example.securing_web.controller;

import com.example.securing_web.entity.Post;
import com.example.securing_web.entity.Category;
import com.example.securing_web.entity.User;
import com.example.securing_web.repository.PostRepository;
import com.example.securing_web.repository.PostVoteRepository;
import com.example.securing_web.repository.UserRepository;
import com.example.securing_web.service.CommentService;
import com.example.securing_web.service.PostService;
import com.example.securing_web.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.HiddenHttpMethodFilter;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

@Controller
@RequestMapping("/posts")
public class PostController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private PostVoteRepository postVoteRepository;

    @Autowired
    private CategoryService categoryService;

    // Фильтр для поддержки PUT/DELETE методов через _method
    @Bean
    public HiddenHttpMethodFilter hiddenHttpMethodFilter() {
        return new HiddenHttpMethodFilter();
    }

    @GetMapping("/create")
    public String showCreatePostForm(@RequestParam(value = "categoryId", required = false) Long categoryId,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);

        // Получаем категории, где пользователь может создавать посты
        List<Category> creatableCategories = categoryService.getCategoriesForUser(user).stream()
                .filter(category -> category.canStudentCreatePosts(user))
                .toList();

        model.addAttribute("creatableCategories", creatableCategories);
        model.addAttribute("post", new Post());
        model.addAttribute("username", username);

        if (categoryId != null) {
            // Проверяем, может ли пользователь создавать посты в этой категории
            if (!categoryService.canUserCreatePostInCategory(user, categoryId)) {
                return "redirect:/posts?error=cannot_create_post";
            }
            model.addAttribute("selectedCategoryId", categoryId);
        }

        return "createPost";
    }

    @PostMapping("/create")
    public String createPost(@ModelAttribute Post post,
                             @RequestParam(value = "categoryId", required = false) Long categoryId,
                             @AuthenticationPrincipal UserDetails userDetails) {

        String author = userDetails.getUsername();
        post.setAuthor(author);

        if (categoryId != null) {
            User user = userRepository.findByUsername(author).orElse(null);

            // Проверяем доступ к категории
            if (!categoryService.canUserCreatePostInCategory(user, categoryId)) {
                return "redirect:/posts/create?error=cannot_create_post";
            }

            Category category = categoryService.findCategoryById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));
            post.setCategory(category);
        }

        postRepository.save(post);
        return "redirect:/posts?success=post_created";
    }

    @GetMapping
    public String viewPosts(@RequestParam(value = "categoryId", required = false) Long categoryId,
                            @AuthenticationPrincipal UserDetails userDetails,
                            Model model) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        model.addAttribute("currentUsername", currentUsername);

        // Получаем категории доступные пользователю
        User user = userRepository.findByUsername(currentUsername).orElse(null);
        List<Category> accessibleCategories = categoryService.getCategoriesForUser(user);
        model.addAttribute("accessibleCategories", accessibleCategories);

        List<Post> posts;

        if (categoryId != null) {
            // Проверяем доступ к категории
            if (!categoryService.canUserAccessCategory(user, categoryId)) {
                return "redirect:/posts?error=access_denied";
            }

            posts = postRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId);
            Category currentCategory = categoryService.findCategoryById(categoryId).orElse(null);
            model.addAttribute("currentCategory", currentCategory);
        } else {
            // Показываем посты из всех доступных категорий
            List<Long> categoryIds = accessibleCategories.stream()
                    .map(Category::getId)
                    .toList();

            if (categoryIds.isEmpty()) {
                posts = new ArrayList<>();
            } else {
                posts = postRepository.findByCategoriesOrdered(categoryIds);
            }
        }

        // ★ ИСПРАВЛЕННЫЙ КОД: Загружаем древовидные комментарии для каждого поста ★
        for (Post post : posts) {
            // Используем безопасный метод из CommentService
            List<com.example.securing_web.entity.Comment> comments = commentService.getCommentTreeByPostId(post.getId());
            post.setComments(comments != null ? comments : new ArrayList<>());
        }

        model.addAttribute("posts", posts);
        model.addAttribute("commentService", commentService); // ★ Важно: добавляем сервис в модель

        return "postList";
    }

    @DeleteMapping("/delete/{id}")
    @Transactional
    public String deletePost(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails userDetails) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found with id: " + id));

        String currentUsername = userDetails.getUsername();

        // Проверяем права: либо автор, либо преподаватель/админ
        boolean isAuthor = post.getAuthor().equals(currentUsername);
        boolean hasPrivilege = hasRequiredRole(userDetails, "ROLE_TEACHER", "ROLE_ADMIN");

        if (!isAuthor && !hasPrivilege) {
            System.err.println("Security Alert: User '" + currentUsername +
                    "' attempted to delete post by '" + post.getAuthor() + "'");
            return "redirect:/posts?error=not_authorized";
        }

        postRepository.delete(post);
        return "redirect:/posts?success=post_deleted";
    }

    @GetMapping("/search")
    public String searchPosts(@RequestParam("query") String query,
                              @RequestParam(value = "categoryId", required = false) Long categoryId,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        model.addAttribute("currentUsername", currentUsername);

        // Получаем категории доступные пользователю
        User user = userRepository.findByUsername(currentUsername).orElse(null);
        List<Category> accessibleCategories = categoryService.getCategoriesForUser(user);
        model.addAttribute("accessibleCategories", accessibleCategories);

        List<Post> posts;

        if (categoryId != null) {
            // Проверяем доступ к категории
            if (!categoryService.canUserAccessCategory(user, categoryId)) {
                return "redirect:/posts?error=access_denied";
            }

            posts = postRepository.searchByCategoryAndQuery(categoryId, query);
            Category currentCategory = categoryService.findCategoryById(categoryId).orElse(null);
            model.addAttribute("currentCategory", currentCategory);
        } else {
            // Ищем по всем доступным категориям
            List<Long> categoryIds = accessibleCategories.stream()
                    .map(Category::getId)
                    .toList();

            if (categoryIds.isEmpty()) {
                posts = new ArrayList<>();
            } else {
                // Нужно будет создать соответствующий метод в репозитории
                posts = postRepository.findByCategoriesOrdered(categoryIds).stream()
                        .filter(post -> post.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                                post.getContent().toLowerCase().contains(query.toLowerCase()))
                        .toList();
            }
        }

        // ★ ИСПРАВЛЕННЫЙ КОД: Загружаем древовидные комментарии для найденных постов ★
        for (Post post : posts) {
            List<com.example.securing_web.entity.Comment> comments = commentService.getCommentTreeByPostId(post.getId());
            post.setComments(comments != null ? comments : new ArrayList<>());
        }

        model.addAttribute("posts", posts);
        model.addAttribute("commentService", commentService); // ★ Важно!
        model.addAttribute("searchQuery", query);

        return "postList";
    }

    // Лайк
    @PostMapping("/{id}/like")
    public String likePost(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        postService.likePost(id, userDetails.getUsername());
        return "redirect:/posts";
    }

    // Дизлайк
    @PostMapping("/{id}/dislike")
    public String dislikePost(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        postService.dislikePost(id, userDetails.getUsername());
        return "redirect:/posts";
    }

    // Добавление комментария к посту
    @PostMapping("/{id}/comments")
    public String addComment(@PathVariable("id") Long postId,
                             @RequestParam String text,
                             @AuthenticationPrincipal UserDetails userDetails) {

        if (text == null || text.trim().isEmpty()) {
            return "redirect:/posts?error=empty_comment&postId=" + postId;
        }

        if (text.length() > 2000) {
            return "redirect:/posts?error=comment_too_long&postId=" + postId;
        }

        String author = userDetails.getUsername();

        try {
            commentService.addComment(postId, author, text.trim());
            return "redirect:/posts?success=comment_added#post-" + postId;
        } catch (IllegalArgumentException e) {
            return "redirect:/posts?error=comment_error&message=" + e.getMessage() + "&postId=" + postId;
        }
    }

    // Ответ на комментарий
    @PostMapping("/{postId}/comments/{commentId}/reply")
    public String replyToComment(@PathVariable("postId") Long postId,
                                 @PathVariable("commentId") Long commentId,
                                 @RequestParam String text,
                                 @AuthenticationPrincipal UserDetails userDetails) {

        if (text == null || text.trim().isEmpty()) {
            return "redirect:/posts?error=empty_reply&postId=" + postId + "#comment-" + commentId;
        }

        if (text.length() > 2000) {
            return "redirect:/posts?error=reply_too_long&postId=" + postId + "#comment-" + commentId;
        }

        String author = userDetails.getUsername();

        try {
            commentService.addReply(commentId, author, text.trim());
            return "redirect:/posts?success=reply_added&postId=" + postId + "#comment-" + commentId;
        } catch (IllegalArgumentException e) {
            return "redirect:/posts?error=reply_error&message=" + e.getMessage() + "&postId=" + postId + "#comment-" + commentId;
        }
    }

    // ★ ДОБАВЛЕНО: Получение поста по ID (для API)
    @GetMapping("/{id}")
    @ResponseBody
    public Post getPostById(@PathVariable Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Post not found"));
    }

    // ★ ДОБАВЛЕНО: Получение комментариев поста (для API)
    @GetMapping("/{id}/comments")
    @ResponseBody
    public List<com.example.securing_web.entity.Comment> getPostComments(@PathVariable Long id) {
        return commentService.getCommentTreeByPostId(id);
    }

    private boolean hasRequiredRole(UserDetails userDetails, String... roles) {
        return userDetails.getAuthorities().stream()
                .anyMatch(authority -> Arrays.asList(roles).contains(authority.getAuthority()));
    }

    // ★ ДОБАВЛЕНО: Получение количества комментариев
    @GetMapping("/{id}/comments/count")
    @ResponseBody
    public int getCommentCount(@PathVariable Long id) {
        return commentService.countTotalComments(id);
    }
}
