package com.example.securing_web.repository;

import com.example.securing_web.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Найти все комментарии к посту (без родителей - корневые комментарии)
    List<Comment> findByPostIdAndParentCommentIsNull(Long postId);

    // Найти все комментарии к посту (включая ответы)
    List<Comment> findByPostId(Long postId);

    // Найти ответы на конкретный комментарий
    List<Comment> findByParentCommentId(Long parentCommentId);

    // ★ ДОБАВЛЕНО: поиск с упорядочиванием ★
    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.parentComment IS NULL ORDER BY c.createdAt DESC")
    List<Comment> findRootCommentsByPostIdOrdered(@Param("postId") Long postId);

    @Query("SELECT c FROM Comment c WHERE c.parentComment.id = :parentId ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentIdOrdered(@Param("parentId") Long parentId);
}