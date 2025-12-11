package com.example.securing_web.controller;

import com.example.securing_web.entity.Comment;
import com.example.securing_web.service.CommentService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

@Controller
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    // ★ ИСПРАВЛЕННЫЙ МЕТОД: автор берется из аутентификации ★
    @PostMapping
    public String addComment(@RequestParam Long postId,
                             @RequestParam String content,
                             @AuthenticationPrincipal UserDetails userDetails) {
        if (postId == null || content == null || content.isEmpty()) {
            return "redirect:/posts?error=emptyfields";
        }

        String author = userDetails.getUsername(); // ★ Автоматически
        commentService.addComment(postId, author, content);
        return "redirect:/posts";
    }

    @GetMapping("/post/{postId}")
    @ResponseBody
    public ResponseEntity<List<Comment>> getCommentsByPost(@PathVariable Long postId) {
        List<Comment> comments = commentService.getCommentsByPostId(postId);
        if (comments == null || comments.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(comments);
    }
}