package com.example.securing_web.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean visibleToAll = true; // Доступен всем

    @Column(nullable = false)
    private boolean studentsCanCreatePosts = true; // Студенты могут создавать посты

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private User teacher; // Куратор раздела (преподаватель)

    @ManyToMany
    @JoinTable(
            name = "category_groups",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<StudentGroup> allowedGroups = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Конструкторы
    public Category() {
    }

    public Category(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isVisibleToAll() { return visibleToAll; }
    public void setVisibleToAll(boolean visibleToAll) { this.visibleToAll = visibleToAll; }

    public boolean isStudentsCanCreatePosts() { return studentsCanCreatePosts; }
    public void setStudentsCanCreatePosts(boolean studentsCanCreatePosts) {
        this.studentsCanCreatePosts = studentsCanCreatePosts;
    }

    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }

    public Set<StudentGroup> getAllowedGroups() { return allowedGroups; }
    public void setAllowedGroups(Set<StudentGroup> allowedGroups) { this.allowedGroups = allowedGroups; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Методы для работы с группами
    public void addAllowedGroup(StudentGroup group) {
        this.allowedGroups.add(group);
        group.getCategories().add(this);
    }

    public void removeAllowedGroup(StudentGroup group) {
        this.allowedGroups.remove(group);
        group.getCategories().remove(this);
    }

    // Вспомогательные методы
    public boolean isAccessibleByStudent(User student) {
        if (visibleToAll) {
            return true;
        }

        if (student == null) {
            return false;
        }

        // Проверяем, состоит ли студент в одной из разрешенных групп
        return student.getGroups().stream()
                .anyMatch(group -> allowedGroups.contains(group));
    }

    public boolean canStudentCreatePosts(User student) {
        if (student == null) {
            return false;
        }

        // Преподаватели и администраторы всегда могут создавать посты
        if (student.isTeacher() || student.isAdmin()) {
            return true;
        }

        // Студенты могут создавать посты, если это разрешено и они имеют доступ
        return studentsCanCreatePosts && isAccessibleByStudent(student);
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", visibleToAll=" + visibleToAll +
                ", allowedGroups=" + allowedGroups.size() +
                '}';
    }
}
