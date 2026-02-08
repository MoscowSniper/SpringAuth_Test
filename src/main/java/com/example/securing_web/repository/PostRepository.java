package com.example.securing_web.repository;

import com.example.securing_web.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    // Существующий метод
    List<Post> findByTitleContainingOrContentContaining(String title, String content);

    // ★ ★ ★ ДОБАВЛЕНО: сортировка по дате создания ★ ★ ★
    List<Post> findAllByOrderByCreatedAtDesc();

    // ★ ★ ★ ДОБАВЛЕНО: поиск с сортировкой ★ ★ ★
    @Query("SELECT p FROM Post p WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY p.createdAt DESC")
    List<Post> searchByQueryOrdered(@Param("query") String query);

    List<Post> findByCategoryIdOrderByCreatedAtDesc(Long categoryId);
    List<Post> findByCategoryIdAndAuthor(Long categoryId, String author);

    @Query("SELECT p FROM Post p WHERE p.category.id = :categoryId " +
            "AND (LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))) " +
            "ORDER BY p.createdAt DESC")
    List<Post> searchByCategoryAndQuery(@Param("categoryId") Long categoryId,
                                        @Param("query") String query);

    @Query("SELECT p FROM Post p WHERE p.category.id IN :categoryIds ORDER BY p.createdAt DESC")
    List<Post> findByCategoriesOrdered(@Param("categoryIds") List<Long> categoryIds);
}