package com.example.securing_web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
    private CommentService commentService;  // <-- добавил CommentService

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

    // Удаление поста (работает с _method="delete")
    @DeleteMapping("/delete/{id}")
    public String deletePost(@PathVariable Long id) {
        postRepository.deleteById(id);
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

    // Удалил дублирующий метод getAllPosts (/posts/posts)
    // Теперь метод viewPosts обрабатывает /posts

    // Добавление комментария к посту
    @PostMapping("/{id}/comments")
    public String addComment(@PathVariable("id") Long postId,
                             @RequestParam String author,
                             @RequestParam String text) {
        commentService.addComment(postId, author, text);
        return "redirect:/posts";
    }
}