package com.example.securing_web;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name") // Добавлено поле fullName
    private String fullName; // Новое поле для полного имени

    public User() {
    }

    public User(String username, String password, String fullName) { // Обновленный конструктор
        this.username = username;
        this.password = password;
        this.fullName = fullName; // Инициализация полного имени
    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() { // Геттер для fullName
        return fullName;
    }

    public void setFullName(String fullName) { // Сеттер для fullName
        this.fullName = fullName;
    }
}
