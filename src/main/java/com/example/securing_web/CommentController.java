package com.example.securing_web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    // Конструктор с @Autowired (не обязательно с последних версий Spring)
    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    // Добавление комментария
    @PostMapping
    public ResponseEntity<Comment> addComment(@RequestBody CommentDto commentDto) {
        if (commentDto == null
                || commentDto.getPostId() == null
                || commentDto.getAuthor() == null
                || commentDto.getAuthor().isEmpty()
                || commentDto.getContent() == null
                || commentDto.getContent().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Comment savedComment = commentService.addComment(
                commentDto.getPostId(),
                commentDto.getAuthor(),
                commentDto.getContent()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(savedComment);
    }

    // Получение комментариев по ID поста
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<Comment>> getCommentsByPost(@PathVariable Long postId) {
        List<Comment> comments = commentService.getCommentsByPostId(postId);
        if (comments == null || comments.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(comments);
    }
}