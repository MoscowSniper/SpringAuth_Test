package com.example.securing_web;

import jakarta.persistence.*;

@Entity
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String content;

    private String author; // Поле для имени автора

    public Post() {
    }

    public Post(String title, String content, String author) { // Измените конструктор
        this.title = title;
        this.content = content;
        this.author = author; // Инициализация автора
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

    public String getAuthor() { // Геттер для автора
        return author;
    }

    public void setAuthor(String author) { // Сеттер для автора
        this.author = author;
    }
}
