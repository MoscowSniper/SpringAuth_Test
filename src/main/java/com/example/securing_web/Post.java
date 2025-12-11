package com.example.securing_web;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 5000)
    private String content;

    private String author;

    @Column(nullable = false)
    private int likes = 0;

    @Column(nullable = false)
    private int dislikes = 0;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    // ★ ДОБАВЛЕНА СВЯЗЬ С ГОЛОСАМИ ★
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostVote> votes = new ArrayList<>();

    public Post() {
    }

    public Post(String title, String content, String author) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.likes = 0;
        this.dislikes = 0;
    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getDislikes() {
        return dislikes;
    }

    public void setDislikes(int dislikes) {
        this.dislikes = dislikes;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    // ★ ДОБАВЛЕНЫ ГЕТТЕРЫ И СЕТТЕРЫ ДЛЯ VOTES ★
    public List<PostVote> getVotes() {
        return votes;
    }

    public void setVotes(List<PostVote> votes) {
        this.votes = votes;
    }

    // Методы для работы с комментариями
    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setPost(this);
    }

    public void removeComment(Comment comment) {
        comments.remove(comment);
        comment.setPost(null);
    }

    // ★ ДОБАВЛЕНЫ МЕТОДЫ ДЛЯ РАБОТЫ С ГОЛОСАМИ ★
    public void addVote(PostVote vote) {
        votes.add(vote);
        vote.setPost(this);
    }

    public void removeVote(PostVote vote) {
        votes.remove(vote);
        vote.setPost(null);
    }

    @Override
    public String toString() {
        return "Post{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", author='" + author + '\'' +
                ", likes=" + likes +
                ", dislikes=" + dislikes +
                ", commentsCount=" + (comments != null ? comments.size() : 0) +
                ", votesCount=" + (votes != null ? votes.size() : 0) +
                '}';
    }
}