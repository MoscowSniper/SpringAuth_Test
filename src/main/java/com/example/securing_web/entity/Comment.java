package com.example.securing_web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment; // ★ ДОБАВЛЕНО: родительский комментарий ★

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> replies = new ArrayList<>(); // ★ ДОБАВЛЕНО: ответы ★

    private String author;

    @Column(length = 2000) // Увеличим длину для комментариев
    private String content;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt; // ★ ДОБАВЛЕНО: время создания ★

    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // ★ ДОБАВЛЕНО: время обновления ★

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Comment() {
    }

    public Comment(Post post, String author, String content) {
        this.post = post;
        this.author = author;
        this.content = content;
    }

    // ★ ДОБАВЛЕНО: конструктор для ответов ★
    public Comment(Post post, Comment parentComment, String author, String content) {
        this.post = post;
        this.parentComment = parentComment;
        this.author = author;
        this.content = content;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }

    public Comment getParentComment() { return parentComment; } // ★ ДОБАВЛЕНО ★
    public void setParentComment(Comment parentComment) { this.parentComment = parentComment; } // ★ ДОБАВЛЕНО ★

    public List<Comment> getReplies() { return replies; } // ★ ДОБАВЛЕНО ★
    public void setReplies(List<Comment> replies) { this.replies = replies; } // ★ ДОБАВЛЕНО ★

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; } // ★ ДОБАВЛЕНО ★
    public LocalDateTime getUpdatedAt() { return updatedAt; } // ★ ДОБАВЛЕНО ★

    // ★ ДОБАВЛЕНО: методы для работы с ответами ★
    public void addReply(Comment reply) {
        replies.add(reply);
        reply.setParentComment(this);
    }

    public void removeReply(Comment reply) {
        replies.remove(reply);
        reply.setParentComment(null);
    }

    // ★ ДОБАВЛЕНО: проверка, является ли комментарий ответом ★
    public boolean isReply() {
        return parentComment != null;
    }
}