package com.example.securing_web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.securing_web.User;

@Controller
@RequestMapping("/posts")
public class PostController {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/create")
    public String showCreatePostForm(Model model) {
        model.addAttribute("post", new Post());

        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        model.addAttribute("username", username);

        return "createPost";
    }

    @PostMapping("/create")
    public String createPost(@ModelAttribute Post post, @AuthenticationPrincipal UserDetails userDetails) {
        String author = userDetails.getUsername(); // Получите имя пользователя
        post.setAuthor(author); // Установите автора
        postRepository.save(post);
        return "redirect:/posts";
    }


    @GetMapping
    public String viewPosts(Model model) {
        List<Post> posts = postRepository.findAll();
        model.addAttribute("posts", posts);
        return "postList";
    }

    @PostMapping("/delete/{id}")
    public String deletePost(@PathVariable Long id) {
        postRepository.deleteById(id);
        return "redirect:/posts";

    }
    @GetMapping("/search")
    public String searchPosts(@RequestParam("query") String query, Model model) {
        List<Post> posts = postRepository.findByTitleContainingOrContentContaining(query, query);
        model.addAttribute("posts", posts);
        return "postList"; // Возвращаем ту же страницу, где отображаются посты
    }

}