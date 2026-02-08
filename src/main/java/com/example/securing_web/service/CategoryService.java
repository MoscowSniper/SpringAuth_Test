package com.example.securing_web.service;

import com.example.securing_web.entity.Category;
import com.example.securing_web.entity.StudentGroup;
import com.example.securing_web.entity.User;
import com.example.securing_web.repository.CategoryRepository;
import com.example.securing_web.repository.StudentGroupRepository;
import com.example.securing_web.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final StudentGroupRepository groupRepository;

    public CategoryService(CategoryRepository categoryRepository,
                           UserRepository userRepository,
                           StudentGroupRepository groupRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> findAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Category> findCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Category> findCategoryByName(String name) {
        return categoryRepository.findByName(name);
    }

    @Transactional
    public Category createCategory(String name, String description,
                                   boolean visibleToAll,
                                   boolean studentsCanCreatePosts,
                                   Long teacherId,
                                   Set<Long> allowedGroupIds) {

        if (categoryRepository.existsByName(name)) {
            throw new IllegalArgumentException("Раздел с таким названием уже существует");
        }

        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setVisibleToAll(visibleToAll);
        category.setStudentsCanCreatePosts(studentsCanCreatePosts);

        if (teacherId != null) {
            User teacher = userRepository.findById(teacherId)
                    .orElseThrow(() -> new IllegalArgumentException("Преподаватель не найден"));
            if (!teacher.isTeacher()) {
                throw new IllegalArgumentException("Пользователь не является преподавателем");
            }
            category.setTeacher(teacher);
        }

        Category savedCategory = categoryRepository.save(category);

        // Добавляем разрешенные группы
        if (allowedGroupIds != null && !allowedGroupIds.isEmpty()) {
            for (Long groupId : allowedGroupIds) {
                StudentGroup group = groupRepository.findById(groupId)
                        .orElseThrow(() -> new IllegalArgumentException("Группа не найдена: " + groupId));
                savedCategory.addAllowedGroup(group);
            }
            categoryRepository.save(savedCategory);
        }

        return savedCategory;
    }

    @Transactional
    public Category updateCategory(Long categoryId, String name, String description,
                                   Boolean visibleToAll, Boolean studentsCanCreatePosts,
                                   Long teacherId, Set<Long> allowedGroupIds) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Раздел не найден"));

        if (name != null && !name.equals(category.getName())) {
            if (categoryRepository.existsByName(name)) {
                throw new IllegalArgumentException("Раздел с таким названием уже существует");
            }
            category.setName(name);
        }

        if (description != null) {
            category.setDescription(description);
        }

        if (visibleToAll != null) {
            category.setVisibleToAll(visibleToAll);
        }

        if (studentsCanCreatePosts != null) {
            category.setStudentsCanCreatePosts(studentsCanCreatePosts);
        }

        if (teacherId != null) {
            User teacher = userRepository.findById(teacherId)
                    .orElseThrow(() -> new IllegalArgumentException("Преподаватель не найден"));
            if (!teacher.isTeacher()) {
                throw new IllegalArgumentException("Пользователь не является преподавателем");
            }
            category.setTeacher(teacher);
        } else if (teacherId == null) {
            category.setTeacher(null);
        }

        // Обновляем разрешенные группы
        if (allowedGroupIds != null) {
            category.getAllowedGroups().clear();
            for (Long groupId : allowedGroupIds) {
                StudentGroup group = groupRepository.findById(groupId)
                        .orElseThrow(() -> new IllegalArgumentException("Группа не найдена: " + groupId));
                category.addAllowedGroup(group);
            }
        }

        return categoryRepository.save(category);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Раздел не найден"));

        // Удаляем связи с группами
        category.getAllowedGroups().clear();
        categoryRepository.save(category);

        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public List<Category> getCategoriesForUser(User user) {
        if (user == null) {
            return categoryRepository.findVisibleToAllCategories();
        }

        if (user.isAdmin() || user.isTeacher()) {
            return categoryRepository.findAll();
        }

        // Для студентов получаем ID групп, в которых они состоят
        List<Long> groupIds = user.getGroups().stream()
                .map(group -> group.getId())
                .toList();

        return categoryRepository.findAccessibleCategories(groupIds);
    }

    @Transactional(readOnly = true)
    public boolean canUserAccessCategory(User user, Long categoryId) {
        if (user == null) {
            return false;
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Раздел не найден"));

        return category.isAccessibleByStudent(user);
    }

    @Transactional(readOnly = true)
    public boolean canUserCreatePostInCategory(User user, Long categoryId) {
        if (user == null) {
            return false;
        }

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Раздел не найден"));

        return category.canStudentCreatePosts(user);
    }

    @Transactional(readOnly = true)
    public List<Category> getCategoriesByTeacher(Long teacherId) {
        return categoryRepository.findByTeacherId(teacherId);
    }

    @Transactional(readOnly = true)
    public long getTotalCategoriesCount() {
        return categoryRepository.countAllCategories();
    }

    @Transactional(readOnly = true)
    public List<Category> getCategoriesWithGroups() {
        return categoryRepository.findAll();
    }
}