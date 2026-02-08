package com.example.securing_web.controller;

import com.example.securing_web.entity.Post;
import com.example.securing_web.repository.PostRepository;
import com.example.securing_web.repository.PostVoteRepository;
import com.example.securing_web.repository.UserRepository;
import com.example.securing_web.service.CommentService;
import com.example.securing_web.service.PostService;
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

    // Фильтр для поддержки PUT/DELETE методов через _method
    @Bean
    public HiddenHttpMethodFilter hiddenHttpMethodFilter() {
        return new HiddenHttpMethodFilter();
    }

    @GetMapping("/create")
    public String showCreatePostForm(Model model) {
        model.addAttribute("post", new Post());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        model.addAttribute("username", username);

        return "createPost";
    }

    @PostMapping("/create")
    public String createPost(@ModelAttribute Post post, @AuthenticationPrincipal UserDetails userDetails) {
        String author = userDetails.getUsername();
        post.setAuthor(author);
        postRepository.save(post);
        return "redirect:/posts?success=post_created";
    }

    @GetMapping
    public String viewPosts(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        model.addAttribute("currentUsername", currentUsername);

        // Получаем посты с сортировкой по дате
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();

        // ★ ИСПРАВЛЕННЫЙ КОД: Загружаем древовидные комментарии для каждого поста ★
        for (Post post : posts) {
            // Используем безопасный метод из CommentService
            List<com.example.securing_web.entity.Comment> comments = commentService.getCommentTreeByPostId(post.getId());
            post.setComments(comments != null ? comments : new java.util.ArrayList<>());
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
    public String searchPosts(@RequestParam("query") String query, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        model.addAttribute("currentUsername", currentUsername);

        List<Post> posts = postRepository.searchByQueryOrdered(query);

        // ★ ИСПРАВЛЕННЫЙ КОД: Загружаем древовидные комментарии для найденных постов ★
        for (Post post : posts) {
            List<com.example.securing_web.entity.Comment> comments = commentService.getCommentTreeByPostId(post.getId());
            post.setComments(comments != null ? comments : new java.util.ArrayList<>());
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
