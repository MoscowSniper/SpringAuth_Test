package com.example.securing_web.controller;

import com.example.securing_web.entity.Category;
import com.example.securing_web.entity.Comment;
import com.example.securing_web.entity.Post;
import com.example.securing_web.repository.CategoryRepository;
import com.example.securing_web.repository.PostRepository;
import com.example.securing_web.repository.PostVoteRepository;
import com.example.securing_web.repository.UserRepository;
import com.example.securing_web.service.CategoryService;
import com.example.securing_web.service.CommentService;
import com.example.securing_web.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

    @Autowired
    private CategoryRepository categoryRepository;

    // ==================== ФОРМА СОЗДАНИЯ ПОСТА ====================

    @GetMapping("/create")
    public String showCreatePostForm(@RequestParam(value = "categoryId", required = false) Long categoryId,
                                     Model model) {
        model.addAttribute("post", new Post());

        // Загружаем все категории
        List<Category> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);

        // Если выбрана категория, устанавливаем ее
        if (categoryId != null) {
            model.addAttribute("selectedCategoryId", categoryId);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        model.addAttribute("username", username);

        return "createPost";
    }

    // ==================== СОЗДАНИЕ ПОСТА ====================

    @PostMapping("/create")
    public String createPost(@ModelAttribute Post post,
                             @RequestParam("categoryId") Long categoryId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {
        String author = userDetails.getUsername();

        try {
            // Находим категорию
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));

            post.setAuthor(author);
            post.setCategory(category);
            postRepository.save(post);

            return "redirect:/posts/" + post.getId() + "/view?success=post_created";

        } catch (IllegalArgumentException e) {
            // Если ошибка, возвращаемся на форму с сообщением
            List<Category> categories = categoryService.getAllCategories();
            model.addAttribute("categories", categories);
            model.addAttribute("selectedCategoryId", categoryId);
            model.addAttribute("error", e.getMessage());

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            model.addAttribute("username", username);

            return "createPost";
        }
    }



    @GetMapping("/{id}/view")
    public String viewPost(@PathVariable Long id, Model model) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пост не найден"));

        List<Comment> comments = commentService.getCommentTreeByPostId(id);

        if (comments == null) {
            comments = new ArrayList<>();
            System.out.println("WARNING: Comments list is null for post " + id);
        } else {
            System.out.println("DEBUG: Loaded " + comments.size() + " comments for post " + id);
        }
        post.setComments(comments);

        if (post.getCategory() != null && post.getCategory().getId() != null) {

            Category category = categoryService.getCategoryById(post.getCategory().getId());
            post.setCategory(category);
        }

        System.out.println("=== VIEW POST DETAILS ===");
        System.out.println("Post ID: " + post.getId());
        System.out.println("Post title: " + post.getTitle());
        System.out.println("Comments count in post object: " +
                (post.getComments() != null ? post.getComments().size() : "null"));

        if (post.getComments() != null && !post.getComments().isEmpty()) {
            System.out.println("First comment author: " + post.getComments().get(0).getAuthor());
            System.out.println("First comment content: " + post.getComments().get(0).getContent());
        }

        model.addAttribute("post", post);
        model.addAttribute("commentService", commentService);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        model.addAttribute("currentUsername", currentUsername);


        return "postDetail";
    }

    // ==================== СПИСОК ВСЕХ ПОСТОВ ====================

    @GetMapping
    public String viewPosts(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        model.addAttribute("currentUsername", currentUsername);

        // Получаем посты с сортировкой по дате
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();

        // Загружаем древовидные комментарии для каждого поста
        for (Post post : posts) {
            List<com.example.securing_web.entity.Comment> comments = commentService.getCommentTreeByPostId(post.getId());
            post.setComments(comments != null ? comments : new ArrayList<>());
        }

        model.addAttribute("posts", posts);
        model.addAttribute("commentService", commentService);

        return "postList";
    }

    // ==================== УДАЛЕНИЕ ПОСТА ====================

    @PostMapping("/delete/{id}")
    @Transactional
    public String deletePost(@PathVariable Long id,
                             @AuthenticationPrincipal UserDetails userDetails) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пост не найден"));

        String currentUsername = userDetails.getUsername();
        if (!post.getAuthor().equals(currentUsername)) {
            System.err.println("Security Alert: User '" + currentUsername +
                    "' attempted to delete post by '" + post.getAuthor() + "'");
            return "redirect:/posts?error=not_authorized";
        }

        Long categoryId = post.getCategory() != null ? post.getCategory().getId() : null;

        postRepository.delete(post);

        if (categoryId != null) {
            return "redirect:/categories/" + categoryId + "?success=post_deleted";
        }
        return "redirect:/posts?success=post_deleted";
    }

    // ==================== ПОИСК ПОСТОВ ====================

    @GetMapping("/search")
    public String searchPosts(@RequestParam("query") String query, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        model.addAttribute("currentUsername", currentUsername);

        List<Post> posts = postRepository.searchByQueryOrdered(query);

        // Загружаем древовидные комментарии для найденных постов
        for (Post post : posts) {
            List<com.example.securing_web.entity.Comment> comments = commentService.getCommentTreeByPostId(post.getId());
            post.setComments(comments != null ? comments : new ArrayList<>());
        }

        model.addAttribute("posts", posts);
        model.addAttribute("commentService", commentService);
        model.addAttribute("searchQuery", query);

        return "postList";
    }

    // ==================== ЛАЙК ПОСТА ====================

    @PostMapping("/{id}/like")
    public String likePost(@PathVariable Long id,
                           @AuthenticationPrincipal UserDetails userDetails) {
        postService.likePost(id, userDetails.getUsername());

        // Редирект обратно на страницу поста
        return "redirect:/posts/" + id + "/view";
    }

    // ==================== ДИЗЛАЙК ПОСТА ====================

    @PostMapping("/{id}/dislike")
    public String dislikePost(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetails userDetails) {
        postService.dislikePost(id, userDetails.getUsername());

        // Редирект обратно на страницу поста
        return "redirect:/posts/" + id + "/view";
    }

    // ==================== ДОБАВЛЕНИЕ КОММЕНТАРИЯ К ПОСТУ ====================

    @PostMapping("/{id}/comments")
    @Transactional // Добавляем транзакцию
    public String addComment(@PathVariable("id") Long postId,
                             @RequestParam String text,
                             @AuthenticationPrincipal UserDetails userDetails,
                             HttpServletRequest request) {

        if (text == null || text.trim().isEmpty()) {
            return "redirect:/posts/" + postId + "/view?error=empty_comment";
        }

        if (text.length() > 2000) {
            return "redirect:/posts/" + postId + "/view?error=comment_too_long";
        }

        String author = userDetails.getUsername();

        System.out.println("=== ADDING COMMENT ===");
        System.out.println("Post ID: " + postId);
        System.out.println("Author: " + author);
        System.out.println("Content length: " + text.length());

        try {
            // 1. Сохраняем комментарий
            Comment comment = commentService.addComment(postId, author, text.trim());

            System.out.println("Comment saved with ID: " + comment.getId());

            // 2. ПРИНУДИТЕЛЬНО обновляем кэш Hibernate
            // Это важно для того, чтобы следующий запрос получил свежие данные
            postRepository.flush(); // Синхронизируем с БД

            // 3. Редирект с якорем на новый комментарий
            return "redirect:/posts/" + postId + "/view?success=comment_added#comment-" + comment.getId();

        } catch (IllegalArgumentException e) {
            System.err.println("Error adding comment: " + e.getMessage());
            return "redirect:/posts/" + postId + "/view?error=comment_error&message=" +
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
    }

    // ==================== ОТВЕТ НА КОММЕНТАРИЙ ====================

    @PostMapping("/{postId}/comments/{commentId}/reply")
    public String replyToComment(@PathVariable("postId") Long postId,
                                 @PathVariable("commentId") Long commentId,
                                 @RequestParam String text,
                                 @AuthenticationPrincipal UserDetails userDetails) {

        if (text == null || text.trim().isEmpty()) {
            return "redirect:/posts/" + postId + "/view?error=empty_reply#comment-" + commentId;
        }

        if (text.length() > 2000) {
            return "redirect:/posts/" + postId + "/view?error=reply_too_long#comment-" + commentId;
        }

        String author = userDetails.getUsername();

        try {
            com.example.securing_web.entity.Comment reply = commentService.addReply(commentId, author, text.trim());
            return "redirect:/posts/" + postId + "/view?success=reply_added#comment-" + reply.getId();
        } catch (IllegalArgumentException e) {
            return "redirect:/posts/" + postId + "/view?error=reply_error&message=" + e.getMessage() + "#comment-" + commentId;
        }
    }

    // ==================== API МЕТОДЫ ====================

    @GetMapping("/{id}")
    @ResponseBody
    public Post getPostById(@PathVariable Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пост не найден"));
    }

    @GetMapping("/{id}/comments")
    @ResponseBody
    public List<com.example.securing_web.entity.Comment> getPostComments(@PathVariable Long id) {
        return commentService.getCommentTreeByPostId(id);
    }

    @GetMapping("/{id}/comments/count")
    @ResponseBody
    public int getCommentCount(@PathVariable Long id) {
        return commentService.countTotalComments(id);
    }

    // ==================== РЕДАКТИРОВАНИЕ ПОСТА ====================

    @GetMapping("/{id}/edit")
    public String showEditPostForm(@PathVariable Long id,
                                   Model model,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пост не найден"));

        String currentUsername = userDetails.getUsername();
        if (!post.getAuthor().equals(currentUsername)) {
            return "redirect:/posts/" + id + "/view?error=not_authorized";
        }

        // Загружаем все категории
        List<Category> categories = categoryService.getAllCategories();

        model.addAttribute("post", post);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategoryId", post.getCategory().getId());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        model.addAttribute("username", username);

        return "editPost";
    }

    @PostMapping("/{id}/edit")
    public String updatePost(@PathVariable Long id,
                             @ModelAttribute Post updatedPost,
                             @RequestParam("categoryId") Long categoryId,
                             @AuthenticationPrincipal UserDetails userDetails) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Пост не найден"));

        String currentUsername = userDetails.getUsername();
        if (!post.getAuthor().equals(currentUsername)) {
            return "redirect:/posts/" + id + "/view?error=not_authorized";
        }

        // Обновляем поля поста
        post.setTitle(updatedPost.getTitle());
        post.setContent(updatedPost.getContent());

        // Обновляем категорию, если она изменилась
        if (!post.getCategory().getId().equals(categoryId)) {
            Category newCategory = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));
            post.setCategory(newCategory);
        }

        postRepository.save(post);

        return "redirect:/posts/" + id + "/view?success=post_updated";
    }

    // ==================== ПОЛУЧЕНИЕ ПОСТОВ ПО КАТЕГОРИИ (API) ====================

    @GetMapping("/category/{categoryId}")
    @ResponseBody
    public List<Post> getPostsByCategory(@PathVariable Long categoryId) {
        return postRepository.findByCategoryId(categoryId);
    }

    // ==================== ПОЛУЧЕНИЕ ПОСТОВ ПО КАТЕГОРИИ С СОРТИРОВКОЙ ====================

    @GetMapping("/category/{categoryId}/ordered")
    @ResponseBody
    public List<Post> getPostsByCategoryOrdered(@PathVariable Long categoryId) {
        return postRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId);
    }

    // ==================== ПРОВЕРКА СУЩЕСТВОВАНИЯ КАТЕГОРИИ ====================

    @GetMapping("/check-category/{categoryId}")
    @ResponseBody
    public boolean checkCategoryExists(@PathVariable Long categoryId) {
        return categoryRepository.existsById(categoryId);
    }

    // ==================== СОЗДАНИЕ ПОСТА БЕЗ КАТЕГОРИИ (ЛЕГАСИ) ====================

    @PostMapping("/create-legacy")
    public String createPostLegacy(@ModelAttribute Post post,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        String author = userDetails.getUsername();

        // Находим первую категорию или создаем дефолтную
        List<Category> categories = categoryService.getAllCategories();
        Category category;

        if (categories.isEmpty()) {
            // Создаем дефолтную категорию
            category = new Category("Общее", "Общие обсуждения");
            categoryRepository.save(category);
        } else {
            category = categories.get(0);
        }

        post.setAuthor(author);
        post.setCategory(category);
        postRepository.save(post);

        return "redirect:/posts/" + post.getId() + "/view?success=post_created";
    }

    // ==================== ПОЛУЧЕНИЕ ПОСТОВ БЕЗ КАТЕГОРИИ ====================

    @GetMapping("/without-category")
    @ResponseBody
    public List<Post> getPostsWithoutCategory() {
        return postRepository.findAll().stream()
                .filter(post -> post.getCategory() == null)
                .toList();
    }

    // ==================== ПРИВЯЗКА КАТЕГОРИИ К ПОСТУ ====================

    @PostMapping("/{postId}/assign-category/{categoryId}")
    @Transactional
    public String assignCategoryToPost(@PathVariable Long postId,
                                       @PathVariable Long categoryId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Пост не найден"));

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));

        post.setCategory(category);
        postRepository.save(post);

        return "redirect:/posts/" + postId + "/view?success=category_assigned";
    }
}