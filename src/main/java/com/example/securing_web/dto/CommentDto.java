package com.example.securing_web.dto;

public class CommentDto {
    private Long postId;
    private String author;
    private String content;

    public CommentDto() {
    }

    public CommentDto(Long postId, String author, String content) {
        this.postId = postId;
        this.author = author;
        this.content = content;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}