package com.example.securing_web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.List;

@Controller
public class PostController {

    private List<Post> posts = new ArrayList<>();

    @GetMapping("/posts/create")
    public String showCreatePostForm(Model model) {
        model.addAttribute("post", new Post());
        return "createPost";
    }

    @PostMapping("/posts/create")
    public String createPost(@ModelAttribute Post post, Model model) {
        posts.add(post);
        model.addAttribute("posts", posts);
        return "postList";
    }

    @GetMapping("/posts")
    public String viewPosts(Model model) {
        model.addAttribute("posts", posts);
        return "postList";
    }
}
