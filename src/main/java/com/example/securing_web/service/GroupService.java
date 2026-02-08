package com.example.securing_web.service;

import com.example.securing_web.entity.StudentGroup;
import com.example.securing_web.entity.User;
import com.example.securing_web.repository.StudentGroupRepository;
import com.example.securing_web.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class GroupService {

    private final StudentGroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupService(StudentGroupRepository groupRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<StudentGroup> findAllGroups() {
        return groupRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<StudentGroup> findGroupById(Long id) {
        return groupRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<StudentGroup> findGroupByName(String name) {
        return groupRepository.findByName(name);
    }

    @Transactional
    public StudentGroup createGroup(String name, String description, Long teacherId) {
        if (groupRepository.existsByName(name)) {
            throw new IllegalArgumentException("Группа с таким названием уже существует");
        }

        StudentGroup group = new StudentGroup();
        group.setName(name);
        group.setDescription(description);

        if (teacherId != null) {
            User teacher = userRepository.findById(teacherId)
                    .orElseThrow(() -> new IllegalArgumentException("Преподаватель не найден"));
            if (!teacher.isTeacher()) {
                throw new IllegalArgumentException("Пользователь не является преподавателем");
            }
            group.setTeacher(teacher);
        }

        return groupRepository.save(group);
    }

    @Transactional
    public StudentGroup updateGroup(Long groupId, String name, String description, Long teacherId) {
        StudentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Группа не найдена"));

        if (name != null && !name.equals(group.getName())) {
            if (groupRepository.existsByName(name)) {
                throw new IllegalArgumentException("Группа с таким названием уже существует");
            }
            group.setName(name);
        }

        if (description != null) {
            group.setDescription(description);
        }

        if (teacherId != null) {
            User teacher = userRepository.findById(teacherId)
                    .orElseThrow(() -> new IllegalArgumentException("Преподаватель не найден"));
            if (!teacher.isTeacher()) {
                throw new IllegalArgumentException("Пользователь не является преподавателем");
            }
            group.setTeacher(teacher);
        } else {
            group.setTeacher(null);
        }

        return groupRepository.save(group);
    }

    @Transactional
    public void deleteGroup(Long groupId) {
        StudentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Группа не найдена"));

        // Удаляем связь со студентами
        for (User student : group.getStudents()) {
            student.getGroups().remove(group);
        }
        group.getStudents().clear();

        groupRepository.delete(group);
    }

    @Transactional
    public void addStudentToGroup(Long groupId, Long studentId) {
        StudentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Группа не найдена"));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Студент не найден"));

        if (!student.isStudent()) {
            throw new IllegalArgumentException("Пользователь не является студентом");
        }

        if (student.getGroups().contains(group)) {
            throw new IllegalArgumentException("Студент уже в этой группе");
        }

        student.addGroup(group);
        userRepository.save(student);
    }

    @Transactional
    public void removeStudentFromGroup(Long groupId, Long studentId) {
        StudentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Группа не найдена"));

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Студент не найден"));

        if (!student.getGroups().contains(group)) {
            throw new IllegalArgumentException("Студент не состоит в этой группе");
        }

        student.removeGroup(group);
        userRepository.save(student);
    }

    @Transactional
    public void addMultipleStudentsToGroup(Long groupId, Set<Long> studentIds) {
        StudentGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Группа не найдена"));

        for (Long studentId : studentIds) {
            User student = userRepository.findById(studentId)
                    .orElseThrow(() -> new IllegalArgumentException("Студент не найден: " + studentId));

            if (!student.isStudent()) {
                throw new IllegalArgumentException("Пользователь не является студентом: " + student.getUsername());
            }

            if (!student.getGroups().contains(group)) {
                student.addGroup(group);
                userRepository.save(student);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<User> getGroupStudents(Long groupId) {
        return userRepository.findByGroupId(groupId);
    }

    @Transactional(readOnly = true)
    public List<User> getStudentsWithoutGroup() {
        return userRepository.findByRoles_Name("ROLE_STUDENT").stream()
                .filter(student -> student.getGroups().isEmpty())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentGroup> getGroupsByTeacher(Long teacherId) {
        return groupRepository.findByTeacherId(teacherId);
    }

    @Transactional(readOnly = true)
    public long getTotalGroupsCount() {
        return groupRepository.countAllGroups();
    }

    @Transactional(readOnly = true)
    public long getTotalStudentsInGroups() {
        return groupRepository.countAllStudentsInGroups();
    }
}