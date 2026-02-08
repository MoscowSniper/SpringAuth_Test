package com.example.securing_web.repository;

import com.example.securing_web.entity.Category;
import com.example.securing_web.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);
    boolean existsByName(String name);

    @Query("SELECT c FROM Category c WHERE c.visibleToAll = true ORDER BY c.name")
    List<Category> findVisibleToAllCategories();

    @Query("SELECT c FROM Category c WHERE c.teacher.id = :teacherId ORDER BY c.name")
    List<Category> findByTeacherId(@Param("teacherId") Long teacherId);

    @Query("SELECT DISTINCT c FROM Category c " +
            "LEFT JOIN c.allowedGroups g " +
            "WHERE c.visibleToAll = true OR g.id IN :groupIds " +
            "ORDER BY c.name")
    List<Category> findAccessibleCategories(@Param("groupIds") List<Long> groupIds);

    @Query("SELECT COUNT(c) FROM Category c")
    long countAllCategories();
}