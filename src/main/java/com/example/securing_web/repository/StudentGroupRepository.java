package com.example.securing_web.repository;

import com.example.securing_web.entity.StudentGroup;
import com.example.securing_web.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentGroupRepository extends JpaRepository<StudentGroup, Long> {

    Optional<StudentGroup> findByName(String name);
    boolean existsByName(String name);

    List<StudentGroup> findByTeacherId(Long teacherId);

    @Query("SELECT g FROM StudentGroup g JOIN g.students s WHERE s.id = :studentId")
    List<StudentGroup> findByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(g) FROM StudentGroup g")
    long countAllGroups();

    @Query("SELECT COUNT(DISTINCT s) FROM StudentGroup g JOIN g.students s")
    long countAllStudentsInGroups();
}
