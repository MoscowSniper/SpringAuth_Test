package com.example.securing_web;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }


    @PostMapping
    public String addComment(@RequestParam Long postId,
                             @RequestParam String author,
                             @RequestParam String content) {
        if (postId == null || author == null || author.isEmpty() || content == null || content.isEmpty()) {
            // Можно добавить обработку ошибок, например, возврат страницы с сообщением
            return "redirect:/posts?error=emptyfields";
        }

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

