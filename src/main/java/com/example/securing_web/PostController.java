package com.example.securing_web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional; // ★ ДОБАВЛЕНО ★

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.HiddenHttpMethodFilter;

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

    // ★ ДОБАВЛЕНО ★
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
        return "redirect:/posts";
    }

    @GetMapping
    public String viewPosts(Model model) {
        List<Post> posts = postRepository.findAll();
        model.addAttribute("posts", posts);
        return "postList";
    }

    // ★ ИСПРАВЛЕННЫЙ МЕТОД УДАЛЕНИЯ ★
    @DeleteMapping("/delete/{id}")
    @Transactional // ★ ВАЖНО: Добавлена транзакция ★
    public String deletePost(@PathVariable Long id) {
        try {
            // Вариант 1: Используем каскадное удаление через сущность
            Post post = postRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));

            // ★ Теперь Hibernate сам удалит связанные голоса и комментарии ★
            postRepository.delete(post);

            // Если все еще будет ошибка, используйте этот код:
            // Вариант 2: Сначала удаляем голоса вручную
            // postVoteRepository.deleteByPostId(id);
            // postRepository.deleteById(id);

        } catch (Exception e) {
            System.err.println("Error deleting post: " + e.getMessage());
            // Можно добавить логику обработки ошибок
        }
        return "redirect:/posts";
    }

    @GetMapping("/search")
    public String searchPosts(@RequestParam("query") String query, Model model) {
        List<Post> posts = postRepository.findByTitleContainingOrContentContaining(query, query);
        model.addAttribute("posts", posts);
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
                             @RequestParam String author,
                             @RequestParam String text) {
        commentService.addComment(postId, author, text);
        return "redirect:/posts";
    }
}